@file:Suppress("ktlint:standard:function-naming")

package de.pflugradts.passbird.adapter.userinterface.hotkey

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import de.pflugradts.passbird.application.GlobalHotkeyAdapterPort
import de.pflugradts.passbird.application.RegisteredGlobalHotkey
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class GlobalHotkeyService(
    private val runtimeEnvironment: RuntimeEnvironment = RuntimeEnvironment(),
    private val windowsRegistrarFactory: () -> PlatformHotkeyRegistrar = { WindowsGlobalHotkeyRegistrar() },
    private val macOsRegistrarFactory: () -> PlatformHotkeyRegistrar = { MacOsGlobalHotkeyRegistrar() },
    private val x11RegistrarFactory: () -> PlatformHotkeyRegistrar = { X11GlobalHotkeyRegistrar() },
) : GlobalHotkeyAdapterPort {
    override fun register(key: Char): RegisteredGlobalHotkey? = registrar().register(key.uppercaseChar())

    private fun registrar() = when {
        runtimeEnvironment.isWindows() -> windowsRegistrarFactory()
        runtimeEnvironment.isMacOs() -> macOsRegistrarFactory()
        runtimeEnvironment.hasX11Display() -> x11RegistrarFactory()
        else -> UnsupportedGlobalHotkeyRegistrar()
    }
}

data class RuntimeEnvironment(
    val osName: String = System.getProperty("os.name").orEmpty(),
    val display: String? = System.getenv("DISPLAY"),
    val waylandDisplay: String? = System.getenv("WAYLAND_DISPLAY"),
) {
    fun isWindows() = osName.lowercase().contains("win")
    fun isMacOs() = osName.lowercase().contains("mac")
    fun hasX11Display() = !display.isNullOrBlank()
}

internal interface PlatformHotkeyRegistrar {
    fun register(key: Char): RegisteredGlobalHotkey?
}

internal class UnsupportedGlobalHotkeyRegistrar : PlatformHotkeyRegistrar {
    override fun register(key: Char) = null
}

internal abstract class BackgroundHotkeyRegistration : RegisteredGlobalHotkey {
    private val nextActions = Semaphore(0)
    private val started = CountDownLatch(1)
    protected val running = AtomicBoolean(true)
    private val registered = AtomicBoolean(false)
    private lateinit var worker: Thread

    protected fun start(name: String, run: () -> Unit) {
        worker = Thread({
            run()
        }, name).apply {
            isDaemon = true
            start()
        }
    }

    protected fun markRegistered(success: Boolean) {
        registered.set(success)
        started.countDown()
    }

    protected fun signalNextAction() {
        nextActions.release()
    }

    fun awaitRegistration() = started.await(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS) && registered.get()

    override fun awaitWithin(milliseconds: Long) = nextActions.tryAcquire(milliseconds, TimeUnit.MILLISECONDS)

    override fun release() {
        if (running.getAndSet(false)) {
            onRelease()
            if (::worker.isInitialized) {
                worker.join(JOIN_TIMEOUT_MILLISECONDS)
            }
        }
    }

    protected open fun onRelease() = Unit

    companion object {
        private const val JOIN_TIMEOUT_MILLISECONDS = 500L
        private const val STARTUP_TIMEOUT_SECONDS = 5L
    }
}

internal class WindowsGlobalHotkeyRegistrar(
    private val user32Factory: () -> Win32User32 = Win32User32::instance,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) : PlatformHotkeyRegistrar {
    override fun register(key: Char): RegisteredGlobalHotkey? =
        WindowsRegistration(key, user32Factory(), sleeper).takeIf(WindowsRegistration::awaitRegistration)

    internal class WindowsRegistration(
        key: Char,
        private val user32: Win32User32,
        private val sleeper: (Long) -> Unit,
    ) : BackgroundHotkeyRegistration() {
        init {
            start("passbird-hotkey-windows") {
                val msg = Win32Message()
                user32.PeekMessage(msg, Pointer.NULL, 0, 0, WIN32_PM_NOREMOVE)
                if (!user32.RegisterHotKey(Pointer.NULL, HOTKEY_ID, WIN32_MOD_CONTROL or WIN32_MOD_SHIFT, key.code)) {
                    markRegistered(false)
                    return@start
                }
                markRegistered(true)
                try {
                    while (running.get()) {
                        while (user32.PeekMessage(msg, Pointer.NULL, 0, 0, WIN32_PM_REMOVE)) {
                            if (msg.message == WIN32_WM_HOTKEY) {
                                signalNextAction()
                            }
                        }
                        sleeper(POLL_INTERVAL_MILLISECONDS)
                    }
                } finally {
                    user32.UnregisterHotKey(Pointer.NULL, HOTKEY_ID)
                }
            }
        }
    }
}

internal class MacOsGlobalHotkeyRegistrar(
    private val carbonKeyCodeResolver: (Char) -> Int? = ::carbonKeyCode,
    private val runtimeFactory: () -> MacOsHotkeyRuntime = ::QuartzMacOsHotkeyRuntime,
) : PlatformHotkeyRegistrar {
    override fun register(key: Char): RegisteredGlobalHotkey? = runCatching {
        carbonKeyCodeResolver(key)
            ?.let { MacOsRegistration(it, runtimeFactory()) }
            ?.takeIf(MacOsRegistration::awaitRegistration)
    }.getOrNull()

    internal class MacOsRegistration(
        private val keyCode: Int,
        private val runtime: MacOsHotkeyRuntime,
    ) : BackgroundHotkeyRegistration() {
        @Volatile
        private var loop: MacOsHotkeyLoop? = null

        init {
            start("passbird-hotkey-macos") {
                val hotkeyLoop = runtime.open(keyCode, ::signalNextAction)
                if (hotkeyLoop == null) {
                    markRegistered(false)
                    return@start
                }
                loop = hotkeyLoop
                markRegistered(true)
                try {
                    while (running.get()) {
                        hotkeyLoop.poll(POLL_INTERVAL_MILLISECONDS)
                    }
                } finally {
                    hotkeyLoop.close()
                }
            }
        }
    }
}

internal class X11GlobalHotkeyRegistrar(
    private val x11Factory: () -> XLib = XLib::instance,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) : PlatformHotkeyRegistrar {
    override fun register(key: Char): RegisteredGlobalHotkey? =
        X11Registration(key, x11Factory(), sleeper).takeIf(X11Registration::awaitRegistration)

    internal class X11Registration(
        key: Char,
        private val x11: XLib,
        private val sleeper: (Long) -> Unit,
    ) : BackgroundHotkeyRegistration() {
        init {
            start("passbird-hotkey-x11") {
                val display = x11.XOpenDisplay(null)
                if (display == null) {
                    markRegistered(false)
                    return@start
                }
                val keySym = x11.XStringToKeysym(key.toString())
                val keyCode = x11.XKeysymToKeycode(display, keySym)
                if (keySym == 0L || keyCode == 0) {
                    x11.XCloseDisplay(display)
                    markRegistered(false)
                    return@start
                }
                val rootWindow = x11.XDefaultRootWindow(display)
                x11.XGrabKey(
                    display,
                    keyCode,
                    X11_CONTROL_MASK or X11_SHIFT_MASK,
                    rootWindow,
                    true,
                    X11_GRAB_MODE_ASYNC,
                    X11_GRAB_MODE_ASYNC,
                )
                x11.XSync(display, false)
                markRegistered(true)
                try {
                    val event = XEvent()
                    while (running.get()) {
                        while (x11.XPending(display) > 0) {
                            x11.XNextEvent(display, event)
                            if (event.type == X11_KEY_PRESS) {
                                signalNextAction()
                            }
                        }
                        sleeper(POLL_INTERVAL_MILLISECONDS)
                    }
                } finally {
                    x11.XUngrabKey(display, keyCode, X11_CONTROL_MASK or X11_SHIFT_MASK, rootWindow)
                    x11.XSync(display, false)
                    x11.XCloseDisplay(display)
                }
            }
        }
    }
}

internal interface Win32User32 : Library {
    fun RegisterHotKey(window: Pointer?, id: Int, fsModifiers: Int, virtualKey: Int): Boolean
    fun UnregisterHotKey(window: Pointer?, id: Int): Boolean
    fun PeekMessage(message: Win32Message, window: Pointer?, minFilter: Int, maxFilter: Int, removeMessage: Int): Boolean

    companion object {
        fun instance(): Win32User32 = Native.load("user32", Win32User32::class.java)
    }
}

internal interface MacOsHotkeyRuntime {
    fun open(keyCode: Int, onNextAction: () -> Unit): MacOsHotkeyLoop?
}

internal interface MacOsHotkeyLoop {
    fun poll(milliseconds: Long)
    fun close()
}

internal class QuartzMacOsHotkeyRuntime(
    private val quartz: Quartz = Quartz.instance(),
    private val coreFoundation: CoreFoundation = CoreFoundation.instance(),
) : MacOsHotkeyRuntime {
    override fun open(keyCode: Int, onNextAction: () -> Unit): MacOsHotkeyLoop? {
        val callback = Quartz.EventTapCallback { _, type, event, _ ->
            if (type == CG_EVENT_KEY_DOWN && event != null) {
                val actualKeyCode = quartz.CGEventGetIntegerValueField(event, CG_KEYBOARD_EVENT_KEYCODE_FIELD).toInt()
                val flags = quartz.CGEventGetFlags(event)
                if (actualKeyCode == keyCode && hasRequiredModifiers(flags)) {
                    onNextAction()
                }
            }
            event
        }
        val tap = quartz.CGEventTapCreate(
            CG_SESSION_EVENT_TAP,
            CG_HEAD_INSERT_EVENT_TAP,
            CG_EVENT_TAP_LISTEN_ONLY,
            1L shl CG_EVENT_KEY_DOWN,
            callback,
            Pointer.NULL,
        ) ?: return null
        val source = coreFoundation.CFMachPortCreateRunLoopSource(Pointer.NULL, tap, 0) ?: run {
            coreFoundation.CFRelease(tap)
            return null
        }
        val mode = coreFoundation.CFStringCreateWithCString(Pointer.NULL, CF_RUN_LOOP_DEFAULT_MODE, CF_STRING_ENCODING_UTF8) ?: run {
            coreFoundation.CFRelease(source)
            coreFoundation.CFRelease(tap)
            return null
        }
        val runLoop = coreFoundation.CFRunLoopGetCurrent()
        coreFoundation.CFRunLoopAddSource(runLoop, source, mode)
        quartz.CGEventTapEnable(tap, true)
        return QuartzMacOsHotkeyLoop(runLoop, mode, source, tap, callback, coreFoundation)
    }

    private fun hasRequiredModifiers(flags: Long) = flags and CG_EVENT_FLAG_MASK_CONTROL != 0L &&
        flags and CG_EVENT_FLAG_MASK_SHIFT != 0L
}

internal class QuartzMacOsHotkeyLoop(
    private val runLoop: Pointer,
    private val mode: Pointer,
    private val source: Pointer,
    private val tap: Pointer,
    @Suppress("UNUSED_PARAMETER") private val callback: Quartz.EventTapCallback,
    private val coreFoundation: CoreFoundation,
) : MacOsHotkeyLoop {
    override fun poll(milliseconds: Long) {
        coreFoundation.CFRunLoopRunInMode(mode, milliseconds.toDouble() / MILLISECONDS_PER_SECOND, true)
    }

    override fun close() {
        coreFoundation.CFRunLoopStop(runLoop)
        coreFoundation.CFRelease(source)
        coreFoundation.CFRelease(tap)
        coreFoundation.CFRelease(mode)
    }

    companion object {
        private const val MILLISECONDS_PER_SECOND = 1000.0
    }
}

internal interface Quartz : Library {
    fun CGEventTapCreate(
        tap: Int,
        place: Int,
        options: Int,
        eventsOfInterest: Long,
        callback: EventTapCallback,
        userInfo: Pointer?,
    ): Pointer?

    fun CGEventTapEnable(tap: Pointer, enable: Boolean)
    fun CGEventGetIntegerValueField(event: Pointer, field: Int): Long
    fun CGEventGetFlags(event: Pointer): Long

    fun interface EventTapCallback : Callback {
        fun callback(proxy: Pointer?, type: Int, event: Pointer?, userData: Pointer?): Pointer?
    }

    companion object {
        fun instance(): Quartz = Native.load("ApplicationServices", Quartz::class.java)
    }
}

internal interface CoreFoundation : Library {
    fun CFMachPortCreateRunLoopSource(allocator: Pointer?, port: Pointer, order: Int): Pointer?
    fun CFRunLoopGetCurrent(): Pointer
    fun CFRunLoopAddSource(runLoop: Pointer, source: Pointer, mode: Pointer)
    fun CFRunLoopRunInMode(mode: Pointer, seconds: Double, returnAfterSourceHandled: Boolean): Int
    fun CFRunLoopStop(runLoop: Pointer)
    fun CFStringCreateWithCString(allocator: Pointer?, value: String, encoding: Int): Pointer?
    fun CFRelease(reference: Pointer?)

    companion object {
        fun instance(): CoreFoundation = Native.load("CoreFoundation", CoreFoundation::class.java)
    }
}

internal interface XLib : Library {
    fun XOpenDisplay(displayName: String?): Pointer?
    fun XDefaultRootWindow(display: Pointer): Long
    fun XStringToKeysym(string: String): Long
    fun XKeysymToKeycode(display: Pointer, keySym: Long): Int
    fun XGrabKey(
        display: Pointer,
        keycode: Int,
        modifiers: Int,
        grabWindow: Long,
        ownerEvents: Boolean,
        pointerMode: Int,
        keyboardMode: Int,
    )

    fun XUngrabKey(display: Pointer, keycode: Int, modifiers: Int, grabWindow: Long)
    fun XPending(display: Pointer): Int
    fun XNextEvent(display: Pointer, event: XEvent)
    fun XSync(display: Pointer, discard: Boolean): Int
    fun XCloseDisplay(display: Pointer): Int

    companion object {
        fun instance(): XLib = Native.load("X11", XLib::class.java)
    }
}

internal class Win32Point : Structure() {
    @JvmField
    var x = 0

    @JvmField
    var y = 0

    override fun getFieldOrder() = listOf("x", "y")
}

internal class Win32Message : Structure() {
    @JvmField
    var window: Pointer? = Pointer.NULL

    @JvmField
    var message = 0

    @JvmField
    var wParam: Pointer? = Pointer.NULL

    @JvmField
    var lParam: Pointer? = Pointer.NULL

    @JvmField
    var time = 0

    @JvmField
    var point = Win32Point()

    override fun getFieldOrder() = listOf("window", "message", "wParam", "lParam", "time", "point")
}

internal class XEvent : Structure() {
    @JvmField
    var type = 0

    @JvmField
    var padding = LongArray(24)

    override fun getFieldOrder() = listOf("type", "padding")
}

internal fun carbonKeyCode(key: Char) = mapOf(
    'A' to 0x00,
    'B' to 0x0B,
    'C' to 0x08,
    'D' to 0x02,
    'E' to 0x0E,
    'F' to 0x03,
    'G' to 0x05,
    'H' to 0x04,
    'I' to 0x22,
    'J' to 0x26,
    'K' to 0x28,
    'L' to 0x25,
    'M' to 0x2E,
    'N' to 0x2D,
    'O' to 0x1F,
    'P' to 0x23,
    'Q' to 0x0C,
    'R' to 0x0F,
    'S' to 0x01,
    'T' to 0x11,
    'U' to 0x20,
    'V' to 0x09,
    'W' to 0x0D,
    'X' to 0x07,
    'Y' to 0x10,
    'Z' to 0x06,
)[key]

private const val CF_RUN_LOOP_DEFAULT_MODE = "kCFRunLoopDefaultMode"
private const val CF_STRING_ENCODING_UTF8 = 0x08000100.toInt()
private const val CG_EVENT_FLAG_MASK_CONTROL = 1L shl 18
private const val CG_EVENT_FLAG_MASK_SHIFT = 1L shl 17
private const val CG_EVENT_KEY_DOWN = 10
private const val CG_EVENT_TAP_LISTEN_ONLY = 1
private const val CG_HEAD_INSERT_EVENT_TAP = 0
private const val CG_KEYBOARD_EVENT_KEYCODE_FIELD = 9
private const val CG_SESSION_EVENT_TAP = 1
private const val HOTKEY_ID = 1
private const val POLL_INTERVAL_MILLISECONDS = 50L
private const val WIN32_MOD_CONTROL = 0x0002
private const val WIN32_MOD_SHIFT = 0x0004
private const val WIN32_PM_NOREMOVE = 0x0000
private const val WIN32_PM_REMOVE = 0x0001
private const val WIN32_WM_HOTKEY = 0x0312
private const val X11_CONTROL_MASK = 1 shl 2
private const val X11_GRAB_MODE_ASYNC = 1
private const val X11_KEY_PRESS = 2
private const val X11_SHIFT_MASK = 1 shl 0
