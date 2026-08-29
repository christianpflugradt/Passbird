@file:Suppress("ktlint:standard:function-naming")

package de.pflugradts.passbird.adapter.userinterface.hotkey

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference
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
    private val carbonFactory: () -> Carbon = Carbon::instance,
) : PlatformHotkeyRegistrar {
    override fun register(key: Char): RegisteredGlobalHotkey? =
        carbonKeyCodeResolver(key)?.let { MacOsRegistration(it, carbonFactory()) }?.takeIf(MacOsRegistration::awaitRegistration)

    internal class MacOsRegistration(
        private val keyCode: Int,
        private val carbon: Carbon,
    ) : BackgroundHotkeyRegistration() {
        private val eventHandler = Carbon.EventHandlerCallback { _, _, _ ->
            signalNextAction()
            0
        }

        init {
            start("passbird-hotkey-macos") {
                val target = carbon.GetApplicationEventTarget() ?: return@start markRegistered(false)
                val eventType = CarbonEventTypeSpec(CARBON_EVENT_CLASS_KEYBOARD, CARBON_EVENT_HOTKEY_PRESSED).apply { write() }
                val handlerRef = PointerByReference()
                if (carbon.InstallEventHandler(target, eventHandler, 1, eventType.pointer, Pointer.NULL, handlerRef) != CARBON_NO_ERR) {
                    markRegistered(false)
                    return@start
                }
                val hotKeyRef = PointerByReference()
                val hotKeyId = CarbonEventHotKeyID(CARBON_SIGNATURE, HOTKEY_ID)
                if (carbon.RegisterEventHotKey(keyCode, CARBON_CONTROL_KEY or CARBON_SHIFT_KEY, hotKeyId, target, 0, hotKeyRef) !=
                    CARBON_NO_ERR
                ) {
                    carbon.RemoveEventHandler(handlerRef.value)
                    markRegistered(false)
                    return@start
                }
                markRegistered(true)
                try {
                    while (running.get()) {
                        val nextEvent = PointerByReference()
                        if (carbon.ReceiveNextEvent(1, eventType.pointer, CARBON_POLL_TIMEOUT_SECONDS, true, nextEvent) == CARBON_NO_ERR) {
                            nextEvent.value?.let { event ->
                                try {
                                    carbon.SendEventToEventTarget(event, target)
                                } finally {
                                    carbon.ReleaseEvent(event)
                                }
                            }
                        }
                    }
                } finally {
                    hotKeyRef.value?.let(carbon::UnregisterEventHotKey)
                    carbon.RemoveEventHandler(handlerRef.value)
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

internal interface Carbon : Library {
    fun GetApplicationEventTarget(): Pointer?
    fun InstallEventHandler(
        target: Pointer,
        handler: EventHandlerCallback,
        eventTypeCount: Int,
        eventTypes: Pointer,
        userData: Pointer?,
        handlerRef: PointerByReference,
    ): Int

    fun RemoveEventHandler(handlerRef: Pointer?): Int
    fun RegisterEventHotKey(
        keyCode: Int,
        modifiers: Int,
        hotKeyId: CarbonEventHotKeyID,
        target: Pointer,
        options: Int,
        hotKeyRef: PointerByReference,
    ): Int

    fun UnregisterEventHotKey(hotKeyRef: Pointer?): Int
    fun ReceiveNextEvent(
        eventTypeCount: Int,
        eventTypes: Pointer,
        timeoutInSeconds: Double,
        pullEvent: Boolean,
        eventRef: PointerByReference,
    ): Int

    fun SendEventToEventTarget(eventRef: Pointer?, target: Pointer): Int
    fun ReleaseEvent(eventRef: Pointer?): Int

    fun interface EventHandlerCallback : Callback {
        fun callback(nextHandler: Pointer?, event: Pointer?, userData: Pointer?): Int
    }

    companion object {
        fun instance(): Carbon = Native.load("Carbon", Carbon::class.java)
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

internal class CarbonEventTypeSpec(eventClass: Int = 0, eventKind: Int = 0) : Structure() {
    @JvmField
    var eventClass = eventClass

    @JvmField
    var eventKind = eventKind

    override fun getFieldOrder() = listOf("eventClass", "eventKind")
}

internal class CarbonEventHotKeyID(signature: Int = 0, id: Int = 0) : Structure() {
    @JvmField
    var signature = signature

    @JvmField
    var id = id

    override fun getFieldOrder() = listOf("signature", "id")
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

private const val CARBON_CONTROL_KEY = 1 shl 12
private const val CARBON_EVENT_CLASS_KEYBOARD = 0x6B657962
private const val CARBON_EVENT_HOTKEY_PRESSED = 6
private const val CARBON_NO_ERR = 0
private const val CARBON_POLL_TIMEOUT_SECONDS = 0.05
private const val CARBON_SHIFT_KEY = 1 shl 9
private const val CARBON_SIGNATURE = 0x50424244
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
