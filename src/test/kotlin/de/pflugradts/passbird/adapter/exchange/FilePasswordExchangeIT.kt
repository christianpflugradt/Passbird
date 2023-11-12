package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
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
    fun `should export and re-import passwords across multiple namespaces`() {
        // given
        val givenPasswordEntry1 = BytePair(Pair(bytesOf("key1"), bytesOf("password1")))
        val givenPasswordEntry2 = BytePair(Pair(bytesOf("key2"), bytesOf("password2")))
        val givenPasswordEntry3 = BytePair(Pair(bytesOf("key3"), bytesOf("password3")))
        val givenPasswordEntry4 = BytePair(Pair(bytesOf("key4"), bytesOf("password4")))
        val givenPasswordEntry5 = BytePair(Pair(bytesOf("key5"), bytesOf("password5")))

        // whe
        filePasswordExchange.send(
            mapOf(
                NamespaceSlot.DEFAULT to listOf(givenPasswordEntry1, givenPasswordEntry2),
                NamespaceSlot.N2 to listOf(givenPasswordEntry3),
                NamespaceSlot.N9 to listOf(givenPasswordEntry4, givenPasswordEntry5),
            ),
        )
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual) hasSize 3 containsKey NamespaceSlot.DEFAULT containsKey NamespaceSlot.N2 containsKey NamespaceSlot.N9
        expectThat(actual[NamespaceSlot.DEFAULT]!!).containsExactlyInAnyOrder(givenPasswordEntry1, givenPasswordEntry2)
        expectThat(actual[NamespaceSlot.N2]!!).containsExactlyInAnyOrder(givenPasswordEntry3)
        expectThat(actual[NamespaceSlot.N9]!!).containsExactlyInAnyOrder(givenPasswordEntry4, givenPasswordEntry5)
    }
}
