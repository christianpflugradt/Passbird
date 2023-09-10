package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrEmpty

internal class MainTest {

    @BeforeEach
    fun setup() {
        System.clearProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)
    }

    @Test
    fun `should set system property and boot launcher`() {
        // given
        val configurationDirectory = "tmp"

        // when
        main(arrayOf(configurationDirectory))

        // then
        verify(exactly = 1) { bootModule(eq(LauncherModule())) }
        expectThat(System.getProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)) isEqualTo configurationDirectory
    }

    @Test
    fun `should not set system property if args is not provided`() {
        // given / when
        main(emptyArray())

        // then
        verify(exactly = 1) { bootModule(eq(LauncherModule())) }
        expectThat(System.getProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)).isNullOrEmpty()
    }
}
