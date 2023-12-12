package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isTrue
import java.io.File
import java.util.UUID

class FilePasswordExchangeIT {

    private val tempExchangeDirectory = UUID.randomUUID().toString()
    private val exchangeFile = tempExchangeDirectory + File.separator + ReadableConfiguration.EXCHANGE_FILENAME
    private val filePasswordExchange = FilePasswordExchange(
        tempExchangeDirectory,
        SystemOperation(),
    )

    @BeforeEach
    fun setup() {
        expectThat(File(tempExchangeDirectory).mkdir()).isTrue()
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(exchangeFile).delete()).isTrue()
        expectThat(File(tempExchangeDirectory).delete()).isTrue()
    }

    @Test
    fun `should export and re-import passwords across multiple nests`() {
        // given
        val givenEgg1 = ShellPair(Pair(shellOf("eggId1"), shellOf("password1")))
        val givenEgg2 = ShellPair(Pair(shellOf("eggId2"), shellOf("password2")))
        val givenEgg3 = ShellPair(Pair(shellOf("eggId3"), shellOf("password3")))
        val givenEgg4 = ShellPair(Pair(shellOf("eggId4"), shellOf("password4")))
        val givenEgg5 = ShellPair(Pair(shellOf("eggId5"), shellOf("password5")))

        // whe
        filePasswordExchange.send(
            mapOf(
                Slot.DEFAULT to listOf(givenEgg1, givenEgg2),
                Slot.N2 to listOf(givenEgg3),
                Slot.N9 to listOf(givenEgg4, givenEgg5),
            ),
        )
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual) hasSize 3 containsKey Slot.DEFAULT containsKey Slot.N2 containsKey Slot.N9
        expectThat(actual[Slot.DEFAULT]!!).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
        expectThat(actual[Slot.N2]!!).containsExactlyInAnyOrder(givenEgg3)
        expectThat(actual[Slot.N9]!!).containsExactlyInAnyOrder(givenEgg4, givenEgg5)
    }
}
