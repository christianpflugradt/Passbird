package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isTrue
import java.io.File
import java.util.UUID

@Tag(INTEGRATION)
class FilePasswordExchangeIntegrationTest {

    private val tempExchangeDirectory = UUID.randomUUID().toString()
    private val exchangeFile = tempExchangeDirectory + File.separator + ReadableConfiguration.EXCHANGE_FILENAME
    private val filePasswordExchange = FilePasswordExchange(SystemOperation())

    @BeforeEach
    fun setup() {
        expectThat(File(tempExchangeDirectory).mkdir()).isTrue()
        mainMocked(arrayOf(tempExchangeDirectory))
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(exchangeFile).delete()).isTrue()
        expectThat(File(tempExchangeDirectory).delete()).isTrue()
    }

    @Test
    fun `should export and re-import passwords across multiple nests`() {
        // given
        val givenEgg1 = ShellPair(shellOf("EggId1"), shellOf("Password1"))
        val givenEgg2 = ShellPair(shellOf("EggId2"), shellOf("Password2"))
        val givenEgg3 = ShellPair(shellOf("EggId3"), shellOf("Password3"))
        val givenEgg4 = ShellPair(shellOf("EggId4"), shellOf("Password4"))
        val givenEgg5 = ShellPair(shellOf("EggId5"), shellOf("Password5"))

        // whe
        filePasswordExchange.send(
            mapOf(
                NestSlot.DEFAULT to listOf(givenEgg1, givenEgg2),
                NestSlot.N2 to listOf(givenEgg3),
                NestSlot.N9 to listOf(givenEgg4, givenEgg5),
            ),
        )
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual) hasSize 3 containsKey NestSlot.DEFAULT containsKey NestSlot.N2 containsKey NestSlot.N9
        expectThat(actual[NestSlot.DEFAULT]!!).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
        expectThat(actual[NestSlot.N2]!!).containsExactlyInAnyOrder(givenEgg3)
        expectThat(actual[NestSlot.N9]!!).containsExactlyInAnyOrder(givenEgg4, givenEgg5)
    }
}
