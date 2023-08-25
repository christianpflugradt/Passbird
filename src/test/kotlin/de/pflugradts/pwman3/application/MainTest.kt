package de.pflugradts.pwman3.application

import com.google.inject.Injector
import de.pflugradts.pwman3.application.boot.Bootable
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration
import de.pflugradts.pwman3.application.util.GuiceInjector
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrEmpty

internal class MainTest {

    private val bootable = mockk<Bootable>(relaxed = true)
    private val guiceInjector = mockk<GuiceInjector>(relaxed = true)
    private val main = Main(guiceInjector)

    @BeforeEach
    fun setup() {
        val injector = mockk<Injector>(relaxed = true)
        every { guiceInjector.create(any()) } returns injector
        every { injector.getInstance(Bootable::class.java) } returns bootable
        System.clearProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)
    }

    @Test
    fun `should set system property and boot launcher`() {
        // given
        val configurationDirectory = "tmp"

        // when
        main.boot(configurationDirectory)

        // then
        verify(exactly = 1) { bootable.boot() }
        expectThat(System.getProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)) isEqualTo configurationDirectory
    }

    @Test
    fun `should not set system property if args is not provided`() {
        // given / when
        main.boot(null)

        // then
        verify(exactly = 1) { bootable.boot() }
        expectThat(System.getProperty(ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY)).isNullOrEmpty()
    }
}
