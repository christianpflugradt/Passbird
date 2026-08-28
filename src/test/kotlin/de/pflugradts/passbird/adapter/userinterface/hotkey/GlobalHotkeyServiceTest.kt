package de.pflugradts.passbird.adapter.userinterface.hotkey

import de.pflugradts.passbird.application.RegisteredGlobalHotkey
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

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
