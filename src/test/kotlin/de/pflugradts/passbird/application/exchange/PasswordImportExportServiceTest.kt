package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.util.stream.Stream

class PasswordImportExportServiceTest {

    private val exchangeFactory = mockk<ExchangeFactory>()
    private val passwordService = mockk<PasswordService>()
    private val importExportService = PasswordImportExportService(exchangeFactory, passwordService)
    private val uri = "any uri"

    @Test
    fun `should peek import key bytes`() {
        // given
        val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key1"), withPasswordBytes = bytesOf("password1"))
        val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key2"), withPasswordBytes = bytesOf("password2"))
        fakeExchangeAdapterPort(
            forExchangeFactory = exchangeFactory,
            withPasswordEntries = listOf(passwordEntry1, passwordEntry2),
        )

        // when
        val actual = importExportService.peekImportKeyBytes(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify { passwordService wasNot Called }
        expectThat(actual.toList()).containsExactlyInAnyOrder(passwordEntry1.viewKey(), passwordEntry2.viewKey())
    }

    @Test
    fun `should import passwords`() {
        // given
        val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key1"), withPasswordBytes = bytesOf("password1"))
        val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key2"), withPasswordBytes = bytesOf("password2"))
        val passwordEntries = listOf(passwordEntry1, passwordEntry2)
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withPasswordEntries = passwordEntries)
        fakePasswordService(instance = passwordService)
        val passwordEntriesSlot = slot<Stream<BytePair>>()

        // when
        importExportService.importPasswordEntries(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify(exactly = 1) { passwordService.putPasswordEntries(capture(passwordEntriesSlot)) }
        expectThat(passwordEntriesSlot.isCaptured).isTrue()
        val actual = passwordEntriesSlot.captured.toList()
        expectThat(actual) hasSize passwordEntries.size
        actual.forEachIndexed { index, it ->
            expectThat(it.value.first) isEqualTo passwordEntries[index].viewKey()
            expectThat(it.value.second) isEqualTo passwordEntries[index].viewPassword()
        }
    }

    @Test
    fun `should export passwords`() {
        // given
        val passwordEntry1 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key1"), withPasswordBytes = bytesOf("password1"))
        val passwordEntry2 = createPasswordEntryForTesting(withKeyBytes = bytesOf("key2"), withPasswordBytes = bytesOf("password2"))
        val passwordEntries = listOf(passwordEntry1, passwordEntry2)
        val exchangeAdapterPort = fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory)
        fakePasswordService(instance = passwordService, withPasswordEntries = passwordEntries)
        val bytesPairsSlot = slot<Stream<BytePair>>()

        // when
        importExportService.exportPasswordEntries(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify(exactly = 1) { exchangeAdapterPort.send(capture(bytesPairsSlot)) }
        expectThat(bytesPairsSlot.isCaptured).isTrue()
        val actual = bytesPairsSlot.captured.toList()
        expectThat(actual) hasSize passwordEntries.size
        actual.forEachIndexed { index, it ->
            expectThat(it.value.first) isEqualTo passwordEntries[index].viewKey()
            expectThat(it.value.second) isEqualTo passwordEntries[index].viewPassword()
        }
    }
}
