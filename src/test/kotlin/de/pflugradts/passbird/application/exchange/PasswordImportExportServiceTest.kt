package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N2
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.N9
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.createNamespaceServiceSpyForTesting
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.util.stream.Stream

class PasswordImportExportServiceTest {

    private val exchangeFactory = mockk<ExchangeFactory>()
    private val passwordService = mockk<PasswordService>()
    private val namespaceService = createNamespaceServiceSpyForTesting()
    private val importExportService = PasswordImportExportService(exchangeFactory, passwordService, namespaceService)
    private val uri = "any uri"

    @Test
    fun `should peek import key bytes`() {
        // given
        val passwordEntries = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withPasswordEntries = passwordEntries)

        // when
        val actual = importExportService.peekImportKeyBytes(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        expectThatActualKeysMatchExpected(actual, passwordEntries)
        verify { passwordService wasNot Called }
    }

    @Test
    fun `should import passwords across multiple namespaces`() {
        // given
        val givenCurrentNamespace = N2
        val passwordEntries = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withPasswordEntries = passwordEntries)
        fakePasswordService(instance = passwordService)
        namespaceService.deploy(bytesOf("n2"), N2)
        namespaceService.updateCurrentNamespace(givenCurrentNamespace)
        val importSlot = mutableListOf<Stream<BytePair>>()

        // when
        importExportService.importPasswordEntries(uri)

        // then
        verify { passwordService.putPasswordEntries(capture(importSlot)) }
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify(exactly = 1) { namespaceService.deploy(bytesOf("Namespace-9"), N9) }
        expectThat(importSlot) hasSize 3
        expectThatActualBytePairsMatchExpected(importSlot[0], passwordEntries.subList(0, 2))
        expectThatActualBytePairsMatchExpected(importSlot[1], passwordEntries.subList(2, 3))
        expectThatActualBytePairsMatchExpected(importSlot[2], passwordEntries.subList(3, 5))
        expectThat(namespaceService.getCurrentNamespace().slot) isEqualTo givenCurrentNamespace
    }

    @Test
    fun `should export passwords across multiple namespaces`() {
        // given
        val givenCurrentNamespace = N2
        val passwordEntries = testData()
        val exchangeAdapterPort = fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory)
        fakePasswordService(instance = passwordService, withPasswordEntries = passwordEntries, withNamespaceService = namespaceService)
        namespaceService.deploy(bytesOf("n2"), N2)
        namespaceService.deploy(bytesOf("n2"), N9)
        namespaceService.updateCurrentNamespace(givenCurrentNamespace)
        val exportSlot = slot<Map<NamespaceSlot, List<BytePair>>>()

        // when
        importExportService.exportPasswordEntries(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify { exchangeAdapterPort.send(capture(exportSlot)) }
        val actual = exportSlot.captured
        expectThatActualBytePairsMatchExpected(actual, passwordEntries)
        expectThat(actual) hasSize 3 containsKey DEFAULT containsKey N2 containsKey N9
        expectThat(namespaceService.getCurrentNamespace().slot) isEqualTo givenCurrentNamespace
    }
}

private fun expectThatActualKeysMatchExpected(actual: Map<NamespaceSlot, List<Bytes>>, expected: List<PasswordEntry>) {
    var index = 0
    actual.keys.forEach { slot ->
        actual[slot]!!.forEach {
            expectThat(it) isEqualTo expected[index++].viewKey()
        }
    }
}
private fun expectThatActualBytePairsMatchExpected(actual: Stream<BytePair>, expected: List<PasswordEntry>) {
    val actualList = actual.toList()
    expectThat(actualList.size) isEqualTo expected.size
    actualList.forEachIndexed { index, _ ->
        expectThat(actualList[index].value.first) isEqualTo expected[index].viewKey()
        expectThat(actualList[index].value.second) isEqualTo expected[index].viewPassword()
    }
}

private fun expectThatActualBytePairsMatchExpected(actual: Map<NamespaceSlot, List<BytePair>>, expected: List<PasswordEntry>) {
    var index = 0
    actual.keys.forEach { slot ->
        actual[slot]!!.forEach {
            expectThat(it.value.first) isEqualTo expected[index].viewKey()
            expectThat(it.value.second) isEqualTo expected[index++].viewPassword()
        }
    }
}

private fun testData() = listOf(
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key1"), withPasswordBytes = bytesOf("password1"), withNamespace = DEFAULT),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key2"), withPasswordBytes = bytesOf("password2"), withNamespace = DEFAULT),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key3"), withPasswordBytes = bytesOf("password3"), withNamespace = N2),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key4"), withPasswordBytes = bytesOf("password4"), withNamespace = N9),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key5"), withPasswordBytes = bytesOf("password5"), withNamespace = N9),
)
