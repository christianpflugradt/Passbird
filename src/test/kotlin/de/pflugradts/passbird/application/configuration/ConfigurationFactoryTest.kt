package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class ConfigurationFactoryTest {

    private val systemOperation = mockk<SystemOperation>()
    private val configurationFactory = ConfigurationFactory(systemOperation)

    @Test
    fun `should create default configuration when configuration file does not exist`() {
        // given / when
        val actual = configurationFactory.loadConfiguration()

        // then
        expectThat(actual.template).isTrue()
        expectThat(actual.application.password.length) isEqualTo 20
    }
}
