package de.pflugradts.passbird.adapter.userinterface.hotkey

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import de.pflugradts.passbird.application.GlobalHotkeyBackend
import de.pflugradts.passbird.application.RegisteredGlobalHotkey
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isSameInstanceAs
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture

class GlobalHotkeyServiceTest {

    @Test
    fun `should use windows registrar on windows`() {
        val expectedRegistration = mockk<RegisteredGlobalHotkey>()
        val windowsRegistrar = RecordingRegistrar(expectedRegistration)
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Windows 11"),
            windowsRegistrarFactory = { windowsRegistrar },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isSameInstanceAs(expectedRegistration)
        expectThat(windowsRegistrar.recordedKeys).containsExactly('P')
    }

    @Test
    fun `should use carbon registrar on mac os when backend is auto`() {
        val expectedRegistration = mockk<RegisteredGlobalHotkey>()
        val carbonRegistrar = RecordingRegistrar(expectedRegistration)
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Mac OS X"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { carbonRegistrar },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isSameInstanceAs(expectedRegistration)
        expectThat(carbonRegistrar.recordedKeys).containsExactly('P')
    }

    @Test
    fun `should prefer carbon registrar over x11 on mac os when backend is auto and display is present`() {
        val expectedRegistration = mockk<RegisteredGlobalHotkey>()
        val carbonRegistrar = RecordingRegistrar(expectedRegistration)
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Mac OS X", display = ":0"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { carbonRegistrar },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isSameInstanceAs(expectedRegistration)
        expectThat(carbonRegistrar.recordedKeys).containsExactly('P')
    }

    @Test
    fun `should use x11 registrar when x11 backend is forced`() {
        val expectedRegistration = mockk<RegisteredGlobalHotkey>()
        val x11Registrar = RecordingRegistrar(expectedRegistration)
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.X11,
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux", display = ":0"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { x11Registrar },
        )

        val actual = service.register('p')

        expectThat(actual).isSameInstanceAs(expectedRegistration)
        expectThat(x11Registrar.recordedKeys).containsExactly('P')
    }

    @Test
    fun `should return null without touching x11 registrar when x11 backend is forced without display`() {
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.X11,
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null without touching quartz registrar when quartz backend is forced on windows`() {
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.QUARTZ,
            runtimeEnvironment = RuntimeEnvironment(osName = "Windows 11"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null without touching carbon registrar when carbon backend is forced on linux`() {
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.CARBON,
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux", display = ":0"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null without touching win32 registrar when win32 backend is forced on mac os`() {
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.WIN32,
            runtimeEnvironment = RuntimeEnvironment(osName = "Mac OS X"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null when no supported runtime is available`() {
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null when forced backend throws`() {
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.WIN32,
            runtimeEnvironment = RuntimeEnvironment(osName = "Windows 11"),
            windowsRegistrarFactory = { error("backend unavailable") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.register('p')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should not prepare startup through quartz registrar when backend is auto on mac os`() {
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Mac OS X"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.prepareOnStartup()

        expectThat(actual).isEqualTo(true)
    }

    @Test
    fun `should prepare startup through quartz registrar when backend is forced`() {
        val quartzRegistrar = RecordingRegistrar(startupPrepared = false)
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.QUARTZ,
            runtimeEnvironment = RuntimeEnvironment(osName = "Mac OS X"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { quartzRegistrar },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.prepareOnStartup()

        expectThat(actual).isEqualTo(false)
        expectThat(quartzRegistrar.prepareCalls).isEqualTo(1)
    }

    @Test
    fun `should keep startup preparation permissive when forced quartz preparation throws`() {
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.QUARTZ,
            runtimeEnvironment = RuntimeEnvironment(osName = "Mac OS X"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("backend unavailable") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.prepareOnStartup()

        expectThat(actual).isEqualTo(true)
    }

    @Test
    fun `should not prepare startup through quartz registrar when quartz backend is forced on windows`() {
        val service = GlobalHotkeyService(
            backend = GlobalHotkeyBackend.QUARTZ,
            runtimeEnvironment = RuntimeEnvironment(osName = "Windows 11"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { error("quartz registrar must not be used") },
            x11RegistrarFactory = { error("x11 registrar must not be used") },
        )

        val actual = service.prepareOnStartup()

        expectThat(actual).isEqualTo(true)
    }

    @Test
    fun `should not prepare startup through hotkey backend on non mac os auto runtime`() {
        val quartzRegistrar = RecordingRegistrar(startupPrepared = false)
        val service = GlobalHotkeyService(
            runtimeEnvironment = RuntimeEnvironment(osName = "Linux", display = ":0"),
            windowsRegistrarFactory = { error("windows registrar must not be used") },
            carbonRegistrarFactory = { error("carbon registrar must not be used") },
            quartzRegistrarFactory = { quartzRegistrar },
            x11RegistrarFactory = { RecordingRegistrar() },
        )

        val actual = service.prepareOnStartup()

        expectThat(actual).isEqualTo(true)
        expectThat(quartzRegistrar.prepareCalls).isEqualTo(0)
    }

    @Test
    fun `should return null when windows registration fails`() {
        val user32 = FakeWin32User32(registerHotKeyResult = false)

        val actual = WindowsGlobalHotkeyRegistrar(user32Factory = { user32 }).register('P')

        expectThat(actual).isEqualTo(null)
        expectThat(user32.registerCalls).isEqualTo(1)
        expectThat(user32.unregisterCalls).isEqualTo(0)
    }

    @Test
    fun `should signal and unregister windows hotkey`() {
        val user32 = FakeWin32User32(peekedMessages = ArrayDeque(listOf(WIN32_HOTKEY_MESSAGE)))
        val registration = WindowsGlobalHotkeyRegistrar(
            user32Factory = { user32 },
            sleeper = {},
        ).register('P')

        expectThat(registration).isSameInstanceAs(registration)
        expectThat(registration?.awaitWithin(250)).isEqualTo(true)

        registration?.release()

        expectThat(user32.unregisterCalls).isEqualTo(1)
    }

    @Test
    fun `should open mac os permission settings during startup when global listening is unavailable`() {
        val runtime = FakeMacOsHotkeyRuntime(canListenGlobally = false)

        val actual = QuartzMacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).prepareOnStartup()

        expectThat(actual).isEqualTo(false)
        expectThat(runtime.openPermissionSettingsCalls).isEqualTo(1)
    }

    @Test
    fun `should not open mac os permission settings during startup when global listening is available`() {
        val runtime = FakeMacOsHotkeyRuntime(canListenGlobally = true)

        val actual = QuartzMacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).prepareOnStartup()

        expectThat(actual).isEqualTo(true)
        expectThat(runtime.openPermissionSettingsCalls).isEqualTo(0)
    }

    @Test
    fun `should return null for unsupported mac os key`() {
        val actual = QuartzMacOsGlobalHotkeyRegistrar(keyCodeResolver = { null }).register('1')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null when quartz mac os hotkey runtime cannot open`() {
        val runtime = FakeMacOsHotkeyRuntime(loop = null)

        val actual = QuartzMacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).register('P')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should signal and unregister quartz mac os hotkey`() {
        val loop = FakeMacOsHotkeyLoop()
        val runtime = FakeMacOsHotkeyRuntime(loop = loop)
        val registration = QuartzMacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).register('P')

        expectThat(registration).isSameInstanceAs(registration)
        expectThat(registration?.awaitWithin(250)).isEqualTo(true)
        expectThat(registration?.awaitWithin(250)).isEqualTo(false)

        registration?.release()

        expectThat(loop.closed).isEqualTo(true)
    }

    @Test
    fun `should return null for unsupported carbon mac os key`() {
        val actual = CarbonMacOsGlobalHotkeyRegistrar(keyCodeResolver = { null }).register('1')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should return null when carbon mac os hotkey runtime cannot open`() {
        val runtime = FakeCarbonMacOsHotkeyRuntime(session = null)

        val actual = CarbonMacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).register('P')

        expectThat(actual).isEqualTo(null)
    }

    @Test
    fun `should signal and unregister carbon mac os hotkey`() {
        val session = FakeCarbonMacOsHotkeySession()
        val runtime = FakeCarbonMacOsHotkeyRuntime(session)
        val registration = CarbonMacOsGlobalHotkeyRegistrar(runtimeFactory = { runtime }).register('P')

        session.onNextAction?.invoke()

        expectThat(registration?.awaitWithin(250)).isEqualTo(true)

        registration?.release()

        expectThat(session.closed).isEqualTo(true)
    }

    @Test
    fun `should remove carbon event handler when hotkey registration fails`() {
        val carbon = FakeCarbon(registerStatus = 1)
        val dispatcher = RecordingMacOsMainThreadDispatcher()

        val actual = CarbonMacOsHotkeyRuntime(carbon, dispatcher).open(35) { }

        expectThat(actual).isEqualTo(null)
        expectThat(dispatcher.dispatchCalls).isEqualTo(1)
        expectThat(carbon.removedEventHandlers).containsExactly(Pointer(52))
    }

    @Test
    fun `should return null when carbon event target is unavailable`() {
        val carbon = FakeCarbon(eventTarget = null)
        val dispatcher = RecordingMacOsMainThreadDispatcher()

        val actual = CarbonMacOsHotkeyRuntime(carbon, dispatcher).open(35) { }

        expectThat(actual).isEqualTo(null)
        expectThat(dispatcher.dispatchCalls).isEqualTo(1)
        expectThat(carbon.installEventHandlerCalls).isEqualTo(0)
    }

    @Test
    fun `should return null when carbon event handler installation fails`() {
        val carbon = FakeCarbon(installStatus = 1)
        val dispatcher = RecordingMacOsMainThreadDispatcher()

        val actual = CarbonMacOsHotkeyRuntime(carbon, dispatcher).open(35) { }

        expectThat(actual).isEqualTo(null)
        expectThat(dispatcher.dispatchCalls).isEqualTo(1)
        expectThat(carbon.registerHotkeyCalls).isEqualTo(0)
    }

    @Test
    fun `should signal and release carbon hotkey session`() {
        val carbon = FakeCarbon()
        val dispatcher = RecordingMacOsMainThreadDispatcher()
        var nextActions = 0

        val session = CarbonMacOsHotkeyRuntime(carbon, dispatcher).open(35) { nextActions++ }
        carbon.eventHandler?.callback(null, null, null)
        session?.close()

        expectThat(nextActions).isEqualTo(1)
        expectThat(dispatcher.dispatchCalls).isEqualTo(2)
        expectThat(carbon.unregisteredHotkeys).containsExactly(Pointer(53))
        expectThat(carbon.removedEventHandlers).containsExactly(Pointer(52))
    }

    @Test
    fun `should execute mac os main thread dispatch immediately on main thread`() {
        val dispatch = FakeDispatch()
        val pthread = FakePThread(isMainThread = true)
        var executions = 0

        val actual = DispatchMacOsMainThreadDispatcher(dispatch, pthread).dispatch {
            executions++
            "ok"
        }

        expectThat(actual).isEqualTo("ok")
        expectThat(executions).isEqualTo(1)
        expectThat(dispatch.dispatchCalls).isEqualTo(0)
    }

    @Test
    fun `should execute mac os main thread dispatch through main queue when off main thread`() {
        val dispatch = FakeDispatch()
        val pthread = FakePThread(isMainThread = false)
        var executions = 0

        val actual = DispatchMacOsMainThreadDispatcher(dispatch, pthread).dispatch {
            executions++
            "ok"
        }

        expectThat(actual).isEqualTo("ok")
        expectThat(executions).isEqualTo(1)
        expectThat(dispatch.dispatchCalls).isEqualTo(1)
        expectThat(dispatch.recordedQueue).isEqualTo(dispatch.mainQueue)
        expectThat(dispatch.recordedContext).isNotNull()
    }

    @Test
    fun `should rethrow failure from mac os main thread dispatch`() {
        val dispatch = FakeDispatch()
        val pthread = FakePThread(isMainThread = false)

        val actual = assertThrows<IllegalStateException> {
            DispatchMacOsMainThreadDispatcher(dispatch, pthread).dispatch {
                error("boom")
            }
        }

        expectThat(actual.message).isEqualTo("boom")
        expectThat(dispatch.dispatchCalls).isEqualTo(1)
    }

    @Test
    fun `should close carbon hotkey loop only once`() {
        val carbon = FakeCarbon()
        val dispatcher = RecordingMacOsMainThreadDispatcher()
        val loop = CarbonMacOsHotkeyLoop(
            carbon = carbon,
            hotkeyReference = Pointer(53),
            eventHandlerReference = Pointer(52),
            eventHandler = Carbon.EventHandler { _, _, _ -> 0 },
            mainThreadDispatcher = dispatcher,
        )

        loop.close()
        loop.close()

        expectThat(dispatcher.dispatchCalls).isEqualTo(1)
        expectThat(carbon.unregisteredHotkeys).containsExactly(Pointer(53))
        expectThat(carbon.removedEventHandlers).containsExactly(Pointer(52))
    }

    @Test
    fun `should close carbon hotkey loop without native handles`() {
        val carbon = FakeCarbon()
        val dispatcher = RecordingMacOsMainThreadDispatcher()
        val loop = CarbonMacOsHotkeyLoop(
            carbon = carbon,
            hotkeyReference = null,
            eventHandlerReference = null,
            eventHandler = Carbon.EventHandler { _, _, _ -> 0 },
            mainThreadDispatcher = dispatcher,
        )

        loop.close()

        expectThat(dispatcher.dispatchCalls).isEqualTo(1)
        expectThat(carbon.unregisteredHotkeys).hasSize(0)
        expectThat(carbon.removedEventHandlers).hasSize(0)
    }

    @Test
    fun `should return false when mac os startup probe cannot create event tap`() {
        val quartz = FakeQuartz(tap = null)

        val actual = QuartzMacOsHotkeyRuntime(
            quartz = quartz,
            coreFoundation = FakeCoreFoundation(),
        ).canListenGlobally()

        expectThat(actual).isEqualTo(false)
    }

    @Test
    fun `should release event tap after successful mac os startup probe`() {
        val tap = Pointer(21)
        val coreFoundation = FakeCoreFoundation()
        val quartz = FakeQuartz(tap = tap)

        val actual = QuartzMacOsHotkeyRuntime(quartz = quartz, coreFoundation = coreFoundation).canListenGlobally()

        expectThat(actual).isEqualTo(true)
        expectThat(coreFoundation.releasedPointers).containsExactly(tap)
    }

    @Test
    fun `should release tap when mac os run loop source cannot be created`() {
        val tap = Pointer(21)
        val coreFoundation = FakeCoreFoundation(source = null)
        val quartz = FakeQuartz(tap = tap)

        val actual = QuartzMacOsHotkeyRuntime(quartz = quartz, coreFoundation = coreFoundation).open(35) { }

        expectThat(actual).isEqualTo(null)
        expectThat(coreFoundation.releasedPointers).containsExactly(tap)
    }

    @Test
    fun `should release tap and source when mac os run loop mode cannot be created`() {
        val tap = Pointer(21)
        val source = Pointer(22)
        val coreFoundation = FakeCoreFoundation(source = source, mode = null)
        val quartz = FakeQuartz(tap = tap)

        val actual = QuartzMacOsHotkeyRuntime(quartz = quartz, coreFoundation = coreFoundation).open(35) { }

        expectThat(actual).isEqualTo(null)
        expectThat(coreFoundation.releasedPointers).containsExactly(source, tap)
    }

    @Test
    fun `should signal only for matching ctrl shift mac os key down event`() {
        val tap = Pointer(21)
        val source = Pointer(22)
        val mode = Pointer(23)
        val event = Pointer(24)
        val coreFoundation = FakeCoreFoundation(source = source, mode = mode)
        val quartz = FakeQuartz(tap = tap)
        var nextActions = 0

        val loop = QuartzMacOsHotkeyRuntime(quartz = quartz, coreFoundation = coreFoundation).open(35) { nextActions++ }

        quartz.callback?.callback(null, MAC_OS_KEY_DOWN_EVENT, event, null)
        quartz.callback?.callback(null, MAC_OS_KEY_DOWN_EVENT, event, null)
        quartz.callback?.callback(null, MAC_OS_KEY_DOWN_EVENT, event, null)
        quartz.callback?.callback(null, 99, event, null)
        quartz.callback?.callback(null, MAC_OS_KEY_DOWN_EVENT, null, null)

        expectThat(loop).isSameInstanceAs(loop)
        expectThat(quartz.enabledTap).isEqualTo(tap)
        expectThat(quartz.enabled).isEqualTo(true)
        expectThat(coreFoundation.addedRunLoop).isEqualTo(coreFoundation.runLoop)
        expectThat(coreFoundation.addedSource).isEqualTo(source)
        expectThat(coreFoundation.addedMode).isEqualTo(mode)
        expectThat(nextActions).isEqualTo(1)
    }

    @Test
    fun `should open mac os permission settings uri`() {
        val startedCommands = mutableListOf<List<String>>()

        QuartzMacOsHotkeyRuntime(
            quartz = FakeQuartz(),
            coreFoundation = FakeCoreFoundation(),
            processStarter = { command ->
                startedCommands += command.toList()
                FakeProcess()
            },
        ).openPermissionSettings()

        expectThat(startedCommands.single()).containsExactly(
            "open",
            "x-apple.systempreferences:com.apple.preference.security?Privacy_ListenEvent",
        )
    }

    @Test
    fun `should poll and close quartz mac os hotkey loop`() {
        val runLoop = Pointer(31)
        val mode = Pointer(32)
        val source = Pointer(33)
        val tap = Pointer(34)
        val coreFoundation = FakeCoreFoundation()
        val loop = QuartzMacOsHotkeyLoop(
            runLoop = runLoop,
            mode = mode,
            source = source,
            tap = tap,
            callback = FakeQuartz.noopCallback,
            coreFoundation = coreFoundation,
        )

        loop.poll(250)
        loop.close()

        expectThat(coreFoundation.runLoopRunMode).isEqualTo(mode)
        expectThat(coreFoundation.runLoopRunSeconds).isEqualTo(0.25)
        expectThat(coreFoundation.runLoopStopCalls).containsExactly(runLoop)
        expectThat(coreFoundation.releasedPointers).containsExactly(source, tap, mode)
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
        expectThat(x11.closeDisplayCalls).isEqualTo(1)
    }

    @Test
    fun `should return null when x11 grab reports bad access`() {
        val x11 = FakeXLib(simulateBadAccessDuringGrab = true)

        val actual = X11GlobalHotkeyRegistrar(x11Factory = { x11 }).register('P')

        expectThat(actual).isEqualTo(null)
        expectThat(x11.grabModifiers).hasSize(4)
        expectThat(x11.ungrabModifiers).hasSize(4)
        expectThat(x11.closeDisplayCalls).isEqualTo(1)
    }

    @Test
    fun `should signal and unregister x11 hotkey for lock modifier combinations`() {
        val x11 = FakeXLib(
            pendingEvents = ArrayDeque(listOf(X11_KEY_PRESS_EVENT)),
            modifierKeycodes = byteArrayOf(0, 0, 0, 0, 0, 0, 77, 0),
        )
        val registration = X11GlobalHotkeyRegistrar(
            x11Factory = { x11 },
            sleeper = {},
        ).register('P')

        expectThat(registration).isSameInstanceAs(registration)
        expectThat(registration?.awaitWithin(250)).isEqualTo(true)

        registration?.release()

        expectThat(x11.grabModifiers).containsExactly(5, 7, 69, 71)
        expectThat(x11.ungrabModifiers).containsExactly(5, 7, 69, 71)
        expectThat(x11.closeDisplayCalls).isEqualTo(1)
        expectThat(x11.syncCalls).isGreaterThanOrEqualTo(2)
    }
}

private class RecordingRegistrar(
    private val registration: RegisteredGlobalHotkey? = mockk(),
    private val startupPrepared: Boolean = true,
) : PlatformHotkeyRegistrar {
    val recordedKeys = mutableListOf<Char>()
    var prepareCalls = 0

    override fun prepareOnStartup(): Boolean {
        prepareCalls++
        return startupPrepared
    }

    override fun register(key: Char): RegisteredGlobalHotkey? {
        recordedKeys += key
        return registration
    }
}

private class RecordingMacOsMainThreadDispatcher : MacOsMainThreadDispatcher {
    var dispatchCalls = 0

    override fun <T> dispatch(work: () -> T): T {
        dispatchCalls++
        return work()
    }
}

private class FakeDispatch : Dispatch {
    val mainQueue = Pointer(61)
    var dispatchCalls = 0
    var recordedQueue: Pointer? = null
    var recordedContext: Pointer? = null

    override fun dispatch_get_main_queue(): Pointer = mainQueue

    override fun dispatch_sync_f(queue: Pointer, context: Pointer?, work: Dispatch.DispatchFunction) {
        dispatchCalls++
        recordedQueue = queue
        recordedContext = context
        work.callback(context)
    }
}

private class FakePThread(
    private val isMainThread: Boolean,
) : PThread {
    override fun pthread_main_np() = if (isMainThread) 1 else 0
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
    private val canListenGlobally: Boolean = true,
    private val loop: FakeMacOsHotkeyLoop? = FakeMacOsHotkeyLoop(),
) : MacOsHotkeyRuntime {
    var openPermissionSettingsCalls = 0

    override fun canListenGlobally() = canListenGlobally

    override fun open(keyCode: Int, onNextAction: () -> Unit): MacOsHotkeyLoop? {
        loop?.onNextAction = onNextAction
        return loop
    }

    override fun openPermissionSettings() {
        openPermissionSettingsCalls++
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

private class FakeCarbonMacOsHotkeyRuntime(
    private val session: FakeCarbonMacOsHotkeySession? = FakeCarbonMacOsHotkeySession(),
) : CarbonHotkeyRuntime {
    override fun open(keyCode: Int, onNextAction: () -> Unit): CarbonMacOsHotkeySession? {
        session?.onNextAction = onNextAction
        return session
    }
}

private class FakeCarbonMacOsHotkeySession : CarbonMacOsHotkeySession {
    var onNextAction: (() -> Unit)? = null
    var closed = false

    override fun close() {
        closed = true
    }
}

private class FakeCarbon(
    private val eventTarget: Pointer? = Pointer(51),
    private val installStatus: Int = 0,
    private val registerStatus: Int = 0,
) : Carbon {
    var eventHandler: Carbon.EventHandler? = null
    val unregisteredHotkeys = mutableListOf<Pointer>()
    val removedEventHandlers = mutableListOf<Pointer>()
    var installEventHandlerCalls = 0
    var registerHotkeyCalls = 0

    override fun GetApplicationEventTarget(): Pointer? = eventTarget

    override fun InstallEventHandler(
        target: Pointer,
        handler: Carbon.EventHandler,
        numTypes: Int,
        eventTypes: CarbonEventTypeSpec,
        userData: Pointer?,
        eventHandlerRef: PointerByReference,
    ): Int {
        installEventHandlerCalls++
        eventHandler = handler
        eventHandlerRef.value = Pointer(52)
        return installStatus
    }

    override fun RegisterEventHotKey(
        keyCode: Int,
        modifiers: Int,
        hotkeyId: CarbonEventHotkeyId.ByValue,
        target: Pointer,
        options: Int,
        hotkeyRef: PointerByReference,
    ): Int {
        registerHotkeyCalls++
        hotkeyRef.value = Pointer(53)
        return registerStatus
    }

    override fun UnregisterEventHotKey(hotkeyRef: Pointer): Int {
        unregisteredHotkeys += hotkeyRef
        return 0
    }

    override fun RemoveEventHandler(eventHandlerRef: Pointer): Int {
        removedEventHandlers += eventHandlerRef
        return 0
    }
}

private class FakeQuartz(
    private val tap: Pointer? = Pointer(41),
) : Quartz {
    var callback: Quartz.EventTapCallback? = null
    var enabledTap: Pointer? = null
    var enabled = false
    var keyCode = 35L
    private val flagsByInvocation = ArrayDeque(
        listOf(
            MAC_OS_CONTROL_FLAG,
            MAC_OS_CONTROL_FLAG or MAC_OS_SHIFT_FLAG,
            0L,
        ),
    )

    override fun CGEventTapCreate(
        tap: Int,
        place: Int,
        options: Int,
        eventsOfInterest: Long,
        callback: Quartz.EventTapCallback,
        userInfo: Pointer?,
    ): Pointer? {
        this.callback = callback
        return this.tap
    }

    override fun CGEventTapEnable(tap: Pointer, enable: Boolean) {
        enabledTap = tap
        enabled = enable
    }

    override fun CGEventGetIntegerValueField(event: Pointer, field: Int): Long = keyCode

    override fun CGEventGetFlags(event: Pointer): Long = flagsByInvocation.removeFirst()

    companion object {
        val noopCallback = Quartz.EventTapCallback { _, _, event, _ -> event }
    }
}

private class FakeCoreFoundation(
    private val source: Pointer? = Pointer(42),
    private val mode: Pointer? = Pointer(43),
) : CoreFoundation {
    val releasedPointers = mutableListOf<Pointer>()
    val runLoop = Pointer(44)
    var addedRunLoop: Pointer? = null
    var addedSource: Pointer? = null
    var addedMode: Pointer? = null
    var runLoopRunMode: Pointer? = null
    var runLoopRunSeconds = 0.0
    val runLoopStopCalls = mutableListOf<Pointer>()

    override fun CFMachPortCreateRunLoopSource(allocator: Pointer?, port: Pointer, order: Int): Pointer? = source

    override fun CFRunLoopGetCurrent(): Pointer = runLoop

    override fun CFRunLoopAddSource(runLoop: Pointer, source: Pointer, mode: Pointer) {
        addedRunLoop = runLoop
        addedSource = source
        addedMode = mode
    }

    override fun CFRunLoopRunInMode(mode: Pointer, seconds: Double, returnAfterSourceHandled: Boolean): Int {
        runLoopRunMode = mode
        runLoopRunSeconds = seconds
        return 0
    }

    override fun CFRunLoopStop(runLoop: Pointer) {
        runLoopStopCalls += runLoop
    }

    override fun CFStringCreateWithCString(allocator: Pointer?, value: String, encoding: Int): Pointer? = mode

    override fun CFRelease(reference: Pointer?) {
        reference?.let(releasedPointers::add)
    }
}

private class FakeXLib(
    private val display: Pointer? = Pointer(2),
    private val keySym: Long = 42L,
    private val keyCode: Int = 9,
    private val pendingEvents: ArrayDeque<Int> = ArrayDeque(),
    modifierKeycodes: ByteArray = byteArrayOf(0, 0, 0, 0, 0, 77, 0, 0),
    private val simulateBadAccessDuringGrab: Boolean = false,
) : XLib {
    var closeDisplayCalls = 0
    val grabModifiers = mutableListOf<Int>()
    val ungrabModifiers = mutableListOf<Int>()
    var syncCalls = 0
    private val modifierPointer = Memory(maxOf(1, modifierKeycodes.size).toLong()).apply {
        write(0, modifierKeycodes, 0, modifierKeycodes.size)
    }
    private val modifierKeymap = XModifierKeymap().apply {
        maxKeysPerModifier = 1
        modifierMap = modifierPointer
    }
    private var activeErrorHandler: XLib.XErrorHandler? = null

    override fun XOpenDisplay(displayName: String?): Pointer? = display

    override fun XDefaultRootWindow(display: Pointer): Long = 7L

    override fun XStringToKeysym(string: String): Long = if (string == "Num_Lock") 77L else keySym

    override fun XKeysymToKeycode(display: Pointer, keySym: Long): Int = if (keySym == 77L) 77 else keyCode

    override fun XGrabKey(
        display: Pointer,
        keycode: Int,
        modifiers: Int,
        grabWindow: Long,
        ownerEvents: Boolean,
        pointerMode: Int,
        keyboardMode: Int,
    ) {
        grabModifiers += modifiers
        if (simulateBadAccessDuringGrab) {
            activeErrorHandler?.callback(display, XErrorEvent().apply { errorCode = 10.toByte() })
        }
    }

    override fun XUngrabKey(display: Pointer, keycode: Int, modifiers: Int, grabWindow: Long) {
        ungrabModifiers += modifiers
    }

    override fun XPending(display: Pointer): Int = if (pendingEvents.isEmpty()) 0 else 1

    override fun XNextEvent(display: Pointer, event: XEvent) {
        event.type = pendingEvents.removeFirst()
    }

    override fun XSetErrorHandler(handler: XLib.XErrorHandler?): XLib.XErrorHandler? =
        activeErrorHandler.also { activeErrorHandler = handler }

    override fun XSync(display: Pointer, discard: Boolean): Int {
        syncCalls++
        return 0
    }

    override fun XCloseDisplay(display: Pointer): Int {
        closeDisplayCalls++
        return 0
    }

    override fun XGetModifierMapping(display: Pointer): XModifierKeymap = modifierKeymap

    override fun XFreeModifiermap(modifiermap: XModifierKeymap): Int = 0
}

private class FakeProcess : Process() {
    override fun destroy() = Unit
    override fun destroyForcibly(): Process = this
    override fun exitValue() = 0
    override fun getErrorStream() = InputStream.nullInputStream()
    override fun getInputStream() = InputStream.nullInputStream()
    override fun getOutputStream() = OutputStream.nullOutputStream()
    override fun isAlive() = false
    override fun onExit(): CompletableFuture<Process> = CompletableFuture.completedFuture(this)
    override fun pid() = 0L
    override fun supportsNormalTermination() = true
    override fun waitFor() = 0
    override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit) = true
}

private const val WIN32_HOTKEY_MESSAGE = 0x0312
private const val MAC_OS_CONTROL_FLAG = 1L shl 18
private const val MAC_OS_SHIFT_FLAG = 1L shl 17
private const val MAC_OS_KEY_DOWN_EVENT = 10
private const val X11_KEY_PRESS_EVENT = 2
