package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.application.BytePair
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isTrue
import java.io.File
import java.util.UUID
import java.util.stream.Stream

internal class FilePasswordExchangeIT {

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
    fun `should export and re-import passwords`() {
        // given
        val givenPasswordEntry1 = BytePair(Pair(bytesOf("key1"), bytesOf("password1")))
        val givenPasswordEntry2 = BytePair(Pair(bytesOf("key2"), bytesOf("password2")))
        val givenPasswordEntry3 = BytePair(Pair(bytesOf("key3"), bytesOf("password3")))

        // when
        filePasswordExchange.send(Stream.of(givenPasswordEntry1, givenPasswordEntry2, givenPasswordEntry3))
        val actual = filePasswordExchange.receive()

        // then
        expectThat(actual.toList()).containsExactlyInAnyOrder(
            givenPasswordEntry1,
            givenPasswordEntry2,
            givenPasswordEntry3,
        )
    }
}
