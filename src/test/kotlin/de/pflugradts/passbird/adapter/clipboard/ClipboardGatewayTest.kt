package de.pflugradts.passbird.adapter.clipboard

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ClipboardGatewayTest {

    private val nativeClipboardGateway = mockk<NativeClipboardGateway>()
    private val awtClipboardGateway = mockk<AwtClipboardGateway>()
    private val clipboardGateway = ClipboardGateway(nativeClipboardGateway, awtClipboardGateway)

    @Test
    fun `should prefer native clipboard tooling when enabled`() {
        every { nativeClipboardGateway.copy("secret") } returns Unit

        clipboardGateway.copy("secret", true)

        verify(exactly = 1) { nativeClipboardGateway.copy("secret") }
        verify(exactly = 0) { awtClipboardGateway.copy(any()) }
    }

    @Test
    fun `should fall back to awt clipboard when native clipboard tooling fails`() {
        every { nativeClipboardGateway.copy("secret") } throws IllegalStateException("native unavailable")
        every { awtClipboardGateway.copy("secret") } returns Unit

        clipboardGateway.copy("secret", true)

        verify(exactly = 1) { nativeClipboardGateway.copy("secret") }
        verify(exactly = 1) { awtClipboardGateway.copy("secret") }
    }

    @Test
    fun `should use awt clipboard directly when native clipboard tooling is disabled`() {
        every { awtClipboardGateway.copy("secret") } returns Unit

        clipboardGateway.copy("secret", false)

        verify(exactly = 0) { nativeClipboardGateway.copy(any()) }
        verify(exactly = 1) { awtClipboardGateway.copy("secret") }
    }

    @Test
    fun `should fail when fallback to awt clipboard also fails`() {
        every { nativeClipboardGateway.copy("secret") } throws IllegalStateException("native unavailable")
        every { awtClipboardGateway.copy("secret") } throws IllegalStateException("awt unavailable")

        assertThrows<IllegalStateException> { clipboardGateway.copy("secret", true) }
    }
}

class NativeClipboardGatewayTest {

    private val clipboardCommandLauncher = mockk<ClipboardCommandLauncher>()
    private val environmentGateway = mockk<EnvironmentGateway>()
    private val operatingSystemGateway = mockk<OperatingSystemGateway>()
    private val nativeClipboardGateway = NativeClipboardGateway(
        clipboardCommandLauncher,
        environmentGateway,
        operatingSystemGateway,
    )

    @Test
    fun `should use pbcopy on mac os`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.MAC_OS
        every { clipboardCommandLauncher.copy(listOf("pbcopy"), "secret") } returns Unit

        nativeClipboardGateway.copy("secret")

        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("pbcopy"), "secret") }
    }

    @Test
    fun `should fall back to another linux utility when preferred utility fails`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.LINUX
        every { environmentGateway.value("WAYLAND_DISPLAY") } returns "wayland-1"
        every { environmentGateway.value("DISPLAY") } returns ":0"
        every { clipboardCommandLauncher.copy(listOf("wl-copy"), "secret") } throws IllegalStateException("missing")
        every { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), "secret") } returns Unit

        nativeClipboardGateway.copy("secret")

        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("wl-copy"), "secret") }
        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), "secret") }
        verify(exactly = 0) { clipboardCommandLauncher.copy(listOf("xsel", "--clipboard", "--input"), any()) }
    }

    @Test
    fun `should use clip on windows`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.WINDOWS
        every { clipboardCommandLauncher.copy(listOf("cmd", "/c", "clip"), "secret") } returns Unit

        nativeClipboardGateway.copy("secret")

        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("cmd", "/c", "clip"), "secret") }
    }

    @Test
    fun `should use xclip when only x11 is available`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.LINUX
        every { environmentGateway.value("WAYLAND_DISPLAY") } returns null
        every { environmentGateway.value("DISPLAY") } returns ":0"
        every { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), "secret") } returns Unit

        nativeClipboardGateway.copy("secret")

        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), "secret") }
        verify(exactly = 0) { clipboardCommandLauncher.copy(listOf("wl-copy"), any()) }
    }

    @Test
    fun `should fall back to xsel when xclip fails on x11`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.LINUX
        every { environmentGateway.value("WAYLAND_DISPLAY") } returns null
        every { environmentGateway.value("DISPLAY") } returns ":0"
        every { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), "secret") } throws
            IllegalStateException("xclip missing")
        every { clipboardCommandLauncher.copy(listOf("xsel", "--clipboard", "--input"), "secret") } returns Unit

        nativeClipboardGateway.copy("secret")

        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), "secret") }
        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("xsel", "--clipboard", "--input"), "secret") }
    }

    @Test
    fun `should try generic linux fallback order when display hints are unavailable`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.LINUX
        every { environmentGateway.value("WAYLAND_DISPLAY") } returns null
        every { environmentGateway.value("DISPLAY") } returns null
        every { clipboardCommandLauncher.copy(listOf("wl-copy"), "secret") } returns Unit

        nativeClipboardGateway.copy("secret")

        verify(exactly = 1) { clipboardCommandLauncher.copy(listOf("wl-copy"), "secret") }
        verify(exactly = 0) { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), any()) }
        verify(exactly = 0) { clipboardCommandLauncher.copy(listOf("xsel", "--clipboard", "--input"), any()) }
    }

    @Test
    fun `should fail with last linux clipboard error when all native tools fail`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.LINUX
        every { environmentGateway.value("WAYLAND_DISPLAY") } returns null
        every { environmentGateway.value("DISPLAY") } returns null
        every { clipboardCommandLauncher.copy(listOf("wl-copy"), "secret") } throws IllegalStateException("wl-copy missing")
        every { clipboardCommandLauncher.copy(listOf("xclip", "-selection", "clipboard"), "secret") } throws
            IllegalStateException("xclip missing")
        every { clipboardCommandLauncher.copy(listOf("xsel", "--clipboard", "--input"), "secret") } throws
            IllegalStateException("xsel missing")

        val actual = assertThrows<IllegalStateException> { nativeClipboardGateway.copy("secret") }

        expectThat(actual.message) isEqualTo "xsel missing"
    }

    @Test
    fun `should fail when no native clipboard utility succeeds`() {
        every { operatingSystemGateway.current() } returns OperatingSystem.UNKNOWN

        assertThrows<IllegalStateException> { nativeClipboardGateway.copy("secret") }
    }
}

class OperatingSystemGatewayTest {

    private val operatingSystemGateway = OperatingSystemGateway()

    @Test
    fun `should detect windows operating systems`() {
        expectThat(detect("Windows 11")) isEqualTo OperatingSystem.WINDOWS
    }

    @Test
    fun `should detect mac operating systems`() {
        expectThat(detect("Mac OS X")) isEqualTo OperatingSystem.MAC_OS
    }

    @Test
    fun `should detect darwin operating systems`() {
        expectThat(detect("Darwin")) isEqualTo OperatingSystem.MAC_OS
    }

    @Test
    fun `should detect linux operating systems`() {
        expectThat(detect("Linux")) isEqualTo OperatingSystem.LINUX
    }

    @Test
    fun `should report unknown operating systems`() {
        expectThat(detect("Plan9")) isEqualTo OperatingSystem.UNKNOWN
    }

    private fun detect(name: String): OperatingSystem {
        val previous = System.getProperty("os.name")
        return try {
            System.setProperty("os.name", name)
            operatingSystemGateway.current()
        } finally {
            System.setProperty("os.name", previous)
        }
    }
}
