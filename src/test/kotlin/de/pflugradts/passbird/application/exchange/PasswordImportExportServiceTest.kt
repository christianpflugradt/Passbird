package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Slot.N2
import de.pflugradts.passbird.domain.model.nest.Slot.N9
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.createNestServiceSpyForTesting
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
    private val nestService = createNestServiceSpyForTesting()
    private val importExportService = PasswordImportExportService(exchangeFactory, passwordService, nestService)
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
    fun `should import passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = N2
        val passwordEntries = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withPasswordEntries = passwordEntries)
        fakePasswordService(instance = passwordService)
        nestService.deploy(bytesOf("n2"), N2)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val importSlot = mutableListOf<Stream<BytePair>>()

        // when
        importExportService.importPasswordEntries(uri)

        // then
        verify { passwordService.putPasswordEntries(capture(importSlot)) }
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify(exactly = 1) { nestService.deploy(bytesOf("Namespace-9"), N9) }
        expectThat(importSlot) hasSize 3
        expectThatActualBytePairsMatchExpected(importSlot[0], passwordEntries.subList(0, 2))
        expectThatActualBytePairsMatchExpected(importSlot[1], passwordEntries.subList(2, 3))
        expectThatActualBytePairsMatchExpected(importSlot[2], passwordEntries.subList(3, 5))
        expectThat(nestService.getCurrentNest().slot) isEqualTo givenCurrentNestSlot
    }

    @Test
    fun `should export passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = N2
        val passwordEntries = testData()
        val exchangeAdapterPort = fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory)
        fakePasswordService(instance = passwordService, withPasswordEntries = passwordEntries, withNestService = nestService)
        nestService.deploy(bytesOf("n2"), N2)
        nestService.deploy(bytesOf("n2"), N9)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val exportSlot = slot<Map<Slot, List<BytePair>>>()

        // when
        importExportService.exportPasswordEntries(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify { exchangeAdapterPort.send(capture(exportSlot)) }
        val actual = exportSlot.captured
        expectThatActualBytePairsMatchExpected(actual, passwordEntries)
        expectThat(actual) hasSize 3 containsKey DEFAULT containsKey N2 containsKey N9
        expectThat(nestService.getCurrentNest().slot) isEqualTo givenCurrentNestSlot
    }
}

private fun expectThatActualKeysMatchExpected(actual: Map<Slot, List<Bytes>>, expected: List<PasswordEntry>) {
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

private fun expectThatActualBytePairsMatchExpected(actual: Map<Slot, List<BytePair>>, expected: List<PasswordEntry>) {
    var index = 0
    actual.keys.forEach { slot ->
        actual[slot]!!.forEach {
            expectThat(it.value.first) isEqualTo expected[index].viewKey()
            expectThat(it.value.second) isEqualTo expected[index++].viewPassword()
        }
    }
}

private fun testData() = listOf(
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key1"), withPasswordBytes = bytesOf("password1"), withNestSlot = DEFAULT),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key2"), withPasswordBytes = bytesOf("password2"), withNestSlot = DEFAULT),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key3"), withPasswordBytes = bytesOf("password3"), withNestSlot = N2),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key4"), withPasswordBytes = bytesOf("password4"), withNestSlot = N9),
    createPasswordEntryForTesting(withKeyBytes = bytesOf("key5"), withPasswordBytes = bytesOf("password5"), withNestSlot = N9),
)
