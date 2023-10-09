package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrEmpty

class MainTest {

    @BeforeEach
    fun setup() {
        System.clearProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)
        mockkStatic(::bootModule)
        every { bootModule(any(LauncherModule::class)) } returns Unit
    }

    @AfterEach
    fun cleanup() { unmockkAll() }

    @Test
    fun `should set system property and boot launcher`() {
        // given
        val configurationDirectory = "tmp"

        // when
        main(arrayOf(configurationDirectory))

        // then
        verify(exactly = 1) { bootModule(any(LauncherModule::class)) }
        expectThat(System.getProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)) isEqualTo configurationDirectory
    }

    @Test
    fun `should not set system property if args is not provided`() {
        // given / when
        main(emptyArray())

        // then
        verify(exactly = 1) { bootModule(any(LauncherModule::class)) }
        expectThat(System.getProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)).isNullOrEmpty()
    }
}
