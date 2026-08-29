package de.pflugradts.passbird.adapter.userinterface.hotkey

import com.sun.jna.Pointer
import de.pflugradts.passbird.application.RegisteredGlobalHotkey
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.ArrayDeque

class GlobalHotkeyServiceTest {

    @Test
    fun `should use windows registrar on windows`() {
        val expectedRegistration = mockk<RegisteredGlobalHotkey>()
        val windowsRegistrar = RecordingRegistrar(expectedRegistration)
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Windows 11"),
            windowsRegistrarFactory = { windowsRegistrar },
            macOsRegistrarFactory = { error("macOS registrar must not be used") },
            x11RegistrarFactory = { error("X11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isSameInstanceAs(expectedRegistration)
        expectThat(windowsRegistrar.recordedKeys.toList()).isEqualTo(listOf('P'))
    }

    @Test
    fun `should use mac os registrar on mac os`() {
        val expectedRegistration = mockk<RegisteredGlobalHotkey>()
        val macOsRegistrar = RecordingRegistrar(expectedRegistration)
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Mac OS X"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            macOsRegistrarFactory = { macOsRegistrar },
            x11RegistrarFactory = { error("X11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isSameInstanceAs(expectedRegistration)
        expectThat(macOsRegistrar.recordedKeys.toList()).isEqualTo(listOf('P'))
    }

    @Test
    fun `should use x11 registrar when display is available on linux`() {
        val expectedRegistration = mockk<RegisteredGlobalHotkey>()
        val x11Registrar = RecordingRegistrar(expectedRegistration)
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux", display = ":0"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            macOsRegistrarFactory = { error("macOS registrar must not be used") },
            x11RegistrarFactory = { x11Registrar },
        )

        val actual = service.register('p')

        expectThat(actual).isSameInstanceAs(expectedRegistration)
        expectThat(x11Registrar.recordedKeys.toList()).isEqualTo(listOf('P'))
    }

    @Test
    fun `should return null when no supported runtime is available`() {
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            macOsRegistrarFactory = { error("macOS registrar must not be used") },
            x11RegistrarFactory = { error("X11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null when display variable is blank`() {
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux", display = " "),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            macOsRegistrarFactory = { error("macOS registrar must not be used") },
            x11RegistrarFactory = { error("X11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null when windows registration fails`() {
        val user32 = FakeWin32User32(registerHotKeyResult = false)

        val actual = WindowsGlobalHotkeyRegistrar(user32Factory = { user32 }).register('P')

        expectThat(actual).isEqualTo(null)
        expectThat(user32.registerCalls) isEqualTo 1
        expectThat(user32.unregisterCalls) isEqualTo 0
    }

    @Test
    fun `should signal and unregister windows hotkey`() {
        val user32 = FakeWin32User32(peekedMessages = ArrayDeque(listOf(WIN32_HOTKEY_MESSAGE)))
        val registration = WindowsGlobalHotkeyRegistrar(
            user32Factory = { user32 },
            sleeper = {},
        ).register('P')

        expectThat(registration).isSameInstanceAs(registration)
        expectThat(registration?.awaitWithin(250)) isEqualTo true

        registration?.release()

        expectThat(user32.unregisterCalls) isEqualTo 1
    }

    @Test
    fun `should return null for unsupported mac os key`() {
        val actual = MacOsGlobalHotkeyRegistrar(carbonKeyCodeResolver = { null }).register('1')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null when mac os hotkey runtime cannot open`() {
        val runtime = FakeMacOsHotkeyRuntime(loop = null)

        val actual = MacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).register('P')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should signal and unregister mac os hotkey`() {
        val loop = FakeMacOsHotkeyLoop()
        val runtime = FakeMacOsHotkeyRuntime(loop)
        val registration = MacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).register('P')

        expectThat(registration).isSameInstanceAs(registration)
        expectThat(registration?.awaitWithin(250)) isEqualTo true
        expectThat(registration?.awaitWithin(250)) isEqualTo false

        registration?.release()
        expectThat(loop.closed) isEqualTo true
    }

    @Test
    fun `should return null when x11 display cannot be opened`() {
        val x11 = FakeXLib(display = null)

        val actual = X11GlobalHotkeyRegistrar(x11Factory = { x11 }).register('P')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should close display when x11 key cannot be resolved`() {
        val x11 = FakeXLib(keySym = 0L)

        val actual = X11GlobalHotkeyRegistrar(x11Factory = { x11 }).register('P')

        expectThat(actual).isEqualTo(null)
        expectThat(x11.closeDisplayCalls) isEqualTo 1
    }

    @Test
    fun `should signal and unregister x11 hotkey`() {
        val x11 = FakeXLib(pendingEvents = ArrayDeque(listOf(X11_KEY_PRESS_EVENT)))
        val registration = X11GlobalHotkeyRegistrar(
            x11Factory = { x11 },
            sleeper = {},
        ).register('P')

        expectThat(registration).isSameInstanceAs(registration)
        expectThat(registration?.awaitWithin(250)) isEqualTo true

        registration?.release()

        expectThat(x11.grabCalls) isEqualTo 1
        expectThat(x11.ungrabCalls) isEqualTo 1
        expectThat(x11.closeDisplayCalls) isEqualTo 1
        expectThat(x11.syncCalls) isGreaterThanOrEqualTo 2
    }
}

private class RecordingRegistrar(
    private val registration: RegisteredGlobalHotkey,
) : PlatformHotkeyRegistrar {
    val recordedKeys = mutableListOf<Char>()

    override fun register(key: Char): RegisteredGlobalHotkey {
        recordedKeys += key
        return registration
    }
}

private class FakeWin32User32(
    private val registerHotKeyResult: Boolean = true,
    private val peekedMessages: ArrayDeque<Int> = ArrayDeque(),
) : Win32User32 {
    var registerCalls = 0
    var unregisterCalls = 0

    override fun RegisterHotKey(window: Pointer?, id: Int, fsModifiers: Int, virtualKey: Int): Boolean {
        registerCalls++
        return registerHotKeyResult
    }

    override fun UnregisterHotKey(window: Pointer?, id: Int): Boolean {
        unregisterCalls++
        return true
    }

    override fun PeekMessage(message: Win32Message, window: Pointer?, minFilter: Int, maxFilter: Int, removeMessage: Int): Boolean {
        if (removeMessage == 1 && peekedMessages.isNotEmpty()) {
            message.message = peekedMessages.removeFirst()
            return true
        }
        return false
    }
}

private class FakeMacOsHotkeyRuntime(
    private val loop: FakeMacOsHotkeyLoop?,
) : MacOsHotkeyRuntime {
    override fun open(keyCode: Int, onNextAction: () -> Unit): MacOsHotkeyLoop? {
        loop?.onNextAction = onNextAction
        return loop
    }
}

private class FakeMacOsHotkeyLoop : MacOsHotkeyLoop {
    var onNextAction: (() -> Unit)? = null
    var closed = false
    private var signaled = false

    override fun poll(milliseconds: Long) {
        if (!signaled) {
            signaled = true
            onNextAction?.invoke()
        }
    }

    override fun close() {
        closed = true
    }
}

private class FakeXLib(
    private val display: Pointer? = Pointer(2),
    private val keySym: Long = 42L,
    private val keyCode: Int = 9,
    private val pendingEvents: ArrayDeque<Int> = ArrayDeque(),
) : XLib {
    var grabCalls = 0
    var ungrabCalls = 0
    var syncCalls = 0
    var closeDisplayCalls = 0

    override fun XOpenDisplay(displayName: String?): Pointer? = display

    override fun XDefaultRootWindow(display: Pointer): Long = 7L

    override fun XStringToKeysym(string: String): Long = keySym

    override fun XKeysymToKeycode(display: Pointer, keySym: Long): Int = keyCode

    override fun XGrabKey(
        display: Pointer,
        keycode: Int,
        modifiers: Int,
        grabWindow: Long,
        ownerEvents: Boolean,
        pointerMode: Int,
        keyboardMode: Int,
    ) {
        grabCalls++
    }

    override fun XUngrabKey(display: Pointer, keycode: Int, modifiers: Int, grabWindow: Long) {
        ungrabCalls++
    }

    override fun XPending(display: Pointer): Int = if (pendingEvents.isEmpty()) 0 else 1

    override fun XNextEvent(display: Pointer, event: XEvent) {
        event.type = pendingEvents.removeFirst()
    }

    override fun XSync(display: Pointer, discard: Boolean): Int {
        syncCalls++
        return 0
    }

    override fun XCloseDisplay(display: Pointer): Int {
        closeDisplayCalls++
        return 0
    }
}

private const val WIN32_HOTKEY_MESSAGE = 0x0312
private const val X11_KEY_PRESS_EVENT = 2
