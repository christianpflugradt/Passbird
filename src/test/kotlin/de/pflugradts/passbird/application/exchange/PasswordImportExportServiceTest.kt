package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Slot.N2
import de.pflugradts.passbird.domain.model.nest.Slot.N9
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
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
    fun `should peek import eggId shells`() {
        // given
        val eggs = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withEggs = eggs)

        // when
        val actual = importExportService.peekImportEggIdShells(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        expectThatActualEggIdsMatchExpected(actual, eggs)
        verify { passwordService wasNot Called }
    }

    @Test
    fun `should import passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = N2
        val eggs = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withEggs = eggs)
        fakePasswordService(instance = passwordService)
        nestService.deploy(shellOf("n2"), N2)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val importSlot = mutableListOf<Stream<ShellPair>>()

        // when
        importExportService.importEggs(uri)

        // then
        verify { passwordService.putEggs(capture(importSlot)) }
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify(exactly = 1) { nestService.deploy(shellOf("Namespace-9"), N9) }
        expectThat(importSlot) hasSize 3
        expectThatActualBytePairsMatchExpected(importSlot[0], eggs.subList(0, 2))
        expectThatActualBytePairsMatchExpected(importSlot[1], eggs.subList(2, 3))
        expectThatActualBytePairsMatchExpected(importSlot[2], eggs.subList(3, 5))
        expectThat(nestService.getCurrentNest().slot) isEqualTo givenCurrentNestSlot
    }

    @Test
    fun `should export passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = N2
        val eggs = testData()
        val exchangeAdapterPort = fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory)
        fakePasswordService(instance = passwordService, withEggs = eggs, withNestService = nestService)
        nestService.deploy(shellOf("n2"), N2)
        nestService.deploy(shellOf("n2"), N9)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val exportSlot = slot<Map<Slot, List<ShellPair>>>()

        // when
        importExportService.exportEggs(uri)

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange(uri) }
        verify { exchangeAdapterPort.send(capture(exportSlot)) }
        val actual = exportSlot.captured
        expectThatActualBytePairsMatchExpected(actual, eggs)
        expectThat(actual) hasSize 3 containsKey DEFAULT containsKey N2 containsKey N9
        expectThat(nestService.getCurrentNest().slot) isEqualTo givenCurrentNestSlot
    }
}

private fun expectThatActualEggIdsMatchExpected(actual: Map<Slot, List<Shell>>, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { slot ->
        actual[slot]!!.forEach {
            expectThat(it) isEqualTo expected[index++].viewEggId()
        }
    }
}
private fun expectThatActualBytePairsMatchExpected(actual: Stream<ShellPair>, expected: List<Egg>) {
    val actualList = actual.toList()
    expectThat(actualList.size) isEqualTo expected.size
    actualList.forEachIndexed { index, _ ->
        expectThat(actualList[index].value.first) isEqualTo expected[index].viewEggId()
        expectThat(actualList[index].value.second) isEqualTo expected[index].viewPassword()
    }
}

private fun expectThatActualBytePairsMatchExpected(actual: Map<Slot, List<ShellPair>>, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { slot ->
        actual[slot]!!.forEach {
            expectThat(it.value.first) isEqualTo expected[index].viewEggId()
            expectThat(it.value.second) isEqualTo expected[index++].viewPassword()
        }
    }
}

private fun testData() = listOf(
    createEggForTesting(withEggIdShell = shellOf("eggId1"), withPasswordShell = shellOf("password1"), withNestSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("eggId2"), withPasswordShell = shellOf("password2"), withNestSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("eggId3"), withPasswordShell = shellOf("password3"), withNestSlot = N2),
    createEggForTesting(withEggIdShell = shellOf("eggId4"), withPasswordShell = shellOf("password4"), withNestSlot = N9),
    createEggForTesting(withEggIdShell = shellOf("eggId5"), withPasswordShell = shellOf("password5"), withNestSlot = N9),
)
