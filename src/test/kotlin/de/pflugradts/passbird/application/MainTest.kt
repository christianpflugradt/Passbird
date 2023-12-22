package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MainTest {

    @BeforeEach
    fun setup() { mockMain() }

    @AfterEach
    fun cleanup() { unmockMain() }

    @Test
    fun `should set home to args and boot launcher`() {
        // given
        val configurationDirectory = "tmp"

        // when
        main(arrayOf(configurationDirectory))

        // then
        verify(exactly = 1) { bootModule(any(LauncherModule::class)) }
        expectThat(Global.homeDirectory) isEqualTo configurationDirectory.toDirectory()
    }

    @Test
    fun `should set home to empty string if args is not provided`() {
        // given / when
        main(emptyArray())

        // then
        verify(exactly = 1) { bootModule(any(LauncherModule::class)) }
        expectThat(Global.homeDirectory) isEqualTo "".toDirectory()
    }
}
