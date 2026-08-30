package de.pflugradts.passbird.application

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class GlobalHotkeyBackendTest {
    @Test
    fun `should expose supported configuration values`() {
        expectThat(GlobalHotkeyBackend.supportedConfigurationValues())
            .containsExactly("auto", "win32", "carbon", "quartz", "x11")
    }

    @Test
    fun `should resolve auto backend policies from runtime`() {
        expectThat(GlobalHotkeyBackend.AUTO.resolvePolicy(osName = "Windows 11").registrarBackend)
            .isEqualTo(GlobalHotkeyRegistrarBackend.WIN32)
        expectThat(GlobalHotkeyBackend.AUTO.resolvePolicy(osName = "Mac OS X").registrarBackend)
            .isEqualTo(GlobalHotkeyRegistrarBackend.CARBON)
        expectThat(GlobalHotkeyBackend.AUTO.resolvePolicy(osName = "Linux", display = ":0").registrarBackend)
            .isEqualTo(GlobalHotkeyRegistrarBackend.X11)
        expectThat(GlobalHotkeyBackend.AUTO.resolvePolicy(osName = "Linux").isSupported)
            .isEqualTo(false)
    }

    @Test
    fun `should resolve explicit backend lifecycle requirements from one policy`() {
        val win32Policy = GlobalHotkeyBackend.WIN32.resolvePolicy(osName = "Windows 11")
        val unsupportedWin32Policy = GlobalHotkeyBackend.WIN32.resolvePolicy(osName = "Linux")
        val supportedX11Policy = GlobalHotkeyBackend.X11.resolvePolicy(osName = "Linux", display = ":0")
        val unsupportedX11Policy = GlobalHotkeyBackend.X11.resolvePolicy(osName = "Linux")
        val quartzPolicy = GlobalHotkeyBackend.QUARTZ.resolvePolicy(osName = "Mac OS X")
        val unsupportedQuartzPolicy = GlobalHotkeyBackend.QUARTZ.resolvePolicy(osName = "Windows 11")
        val carbonPolicy = GlobalHotkeyBackend.CARBON.resolvePolicy(osName = "Mac OS X")
        val unsupportedCarbonPolicy = GlobalHotkeyBackend.CARBON.resolvePolicy(osName = "Linux")

        expectThat(win32Policy.registrarBackend).isEqualTo(GlobalHotkeyRegistrarBackend.WIN32)
        expectThat(unsupportedWin32Policy.isSupported).isEqualTo(false)
        expectThat(supportedX11Policy.registrarBackend).isEqualTo(GlobalHotkeyRegistrarBackend.X11)
        expectThat(unsupportedX11Policy.isSupported).isEqualTo(false)
        expectThat(quartzPolicy.registrarBackend).isEqualTo(GlobalHotkeyRegistrarBackend.QUARTZ)
        expectThat(quartzPolicy.preparesOnStartup).isEqualTo(true)
        expectThat(unsupportedQuartzPolicy.isSupported).isEqualTo(false)
        expectThat(quartzPolicy.requiresMacOsApplicationLoop(startsOnFirstThread = true)).isEqualTo(false)
        expectThat(quartzPolicy.requiresMacOsApplicationLoop(startsOnFirstThread = false)).isEqualTo(false)
        expectThat(carbonPolicy.requiresMacOsApplicationLoop(startsOnFirstThread = true)).isEqualTo(true)
        expectThat(carbonPolicy.requiresMacOsApplicationLoop(startsOnFirstThread = false)).isEqualTo(false)
        expectThat(unsupportedCarbonPolicy.isSupported).isEqualTo(false)
    }

    @Test
    fun `should parse backend from configuration value`() {
        expectThat(GlobalHotkeyBackend.fromConfiguration("auto")).isEqualTo(GlobalHotkeyBackend.AUTO)
        expectThat(GlobalHotkeyBackend.fromConfiguration("win32")).isEqualTo(GlobalHotkeyBackend.WIN32)
        expectThat(GlobalHotkeyBackend.fromConfiguration("carbon")).isEqualTo(GlobalHotkeyBackend.CARBON)
        expectThat(GlobalHotkeyBackend.fromConfiguration("quartz")).isEqualTo(GlobalHotkeyBackend.QUARTZ)
        expectThat(GlobalHotkeyBackend.fromConfiguration("x11")).isEqualTo(GlobalHotkeyBackend.X11)
        expectThat(GlobalHotkeyBackend.fromConfiguration("unsupported")).isEqualTo(null)
    }
}
