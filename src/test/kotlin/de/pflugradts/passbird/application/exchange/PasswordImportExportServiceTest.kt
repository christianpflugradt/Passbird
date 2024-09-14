package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.PasswordInfoMap
import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S9
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.nest.createNestServiceSpyForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.util.function.Supplier

class PasswordImportExportServiceTest {

    private val exchangeFactory = mockk<ExchangeFactory>()
    private val passwordService = mockk<PasswordService>()
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val nestService = createNestServiceSpyForTesting()
    private val passbirdHomeUri = "any uri"
    private val importExportServiceSupplier get() =
        Supplier { PasswordImportExportService(exchangeFactory, passwordService, nestService, eventRegistry) }

    @BeforeEach
    fun setup() {
        mainMocked(arrayOf(passbirdHomeUri))
    }

    @Test
    fun `should peek import eggId shells`() {
        // given
        val eggs = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withEggs = eggs)

        // when
        val actual = importExportServiceSupplier.get().peekImportEggIdShells()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        expectThatActualEggIdsMatchExpected(actual, eggs)
        verify { passwordService wasNot Called }
    }

    @Test
    fun `should import passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = S2
        val eggs = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withEggs = eggs)
        fakePasswordService(instance = passwordService)
        nestService.place(shellOf("n2"), S2)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val eggIdSlot = mutableListOf<Shell>()
        val passwordSlot = mutableListOf<Shell>()
        val eggCountSlot = slot<EggsImported>()

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        verify { passwordService.putEgg(capture(eggIdSlot), capture(passwordSlot)) }
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify(exactly = 1) { nestService.place(shellOf(S9.name), S9) }
        expectThat(eggIdSlot) hasSize eggs.size
        expectThat(passwordSlot) hasSize eggs.size
        eggs.indices.forEach { i ->
            expectThat(eggIdSlot[i]) isEqualTo eggs[i].viewEggId()
            expectThat(passwordSlot[i]) isEqualTo eggs[i].viewPassword()
        }
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
        verify { eventRegistry.register(capture(eggCountSlot)) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(eggCountSlot.isCaptured)
        expectThat(eggCountSlot.captured.count) isEqualTo testData().size
    }

    @Test
    fun `should export passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = S2
        val eggs = testData()
        val exchangeAdapterPort = fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory)
        fakePasswordService(instance = passwordService, withEggs = eggs, withNestService = nestService)
        nestService.place(shellOf("n2"), S2)
        nestService.place(shellOf("n9"), S9)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val exportNestSlot = slot<PasswordInfoMap>()
        val eggCountSlot = slot<EggsExported>()

        // when
        importExportServiceSupplier.get().exportEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify { exchangeAdapterPort.send(capture(exportNestSlot)) }
        val actual = exportNestSlot.captured
        expectThatActualBytePairsMatchExpected(actual, eggs)
        expectThat(actual) hasSize 3 containsKey DEFAULT.toNest() containsKey S2.toNest() containsKey S9.toNest()
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
        verify { eventRegistry.register(capture(eggCountSlot)) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(eggCountSlot.isCaptured)
        expectThat(eggCountSlot.captured.count) isEqualTo testData().size
    }

    private fun Slot.toNest() = nestService.atNestSlot(this).get()
}

private fun expectThatActualEggIdsMatchExpected(actual: ShellMap, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { nestSlot ->
        actual[nestSlot]!!.forEach {
            expectThat(it) isEqualTo expected[index++].viewEggId()
        }
    }
}

private fun expectThatActualBytePairsMatchExpected(actual: PasswordInfoMap, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { nestSlot ->
        actual[nestSlot]!!.forEach {
            expectThat(it.first.first) isEqualTo expected[index].viewEggId()
            expectThat(it.first.second) isEqualTo expected[index++].viewPassword()
        }
    }
}

private fun testData() = listOf(
    createEggForTesting(withEggIdShell = shellOf("EggId1"), withPasswordShell = shellOf("Password1"), withSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId2"), withPasswordShell = shellOf("Password2"), withSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId3"), withPasswordShell = shellOf("Password3"), withSlot = S2),
    createEggForTesting(withEggIdShell = shellOf("EggId4"), withPasswordShell = shellOf("Password4"), withSlot = S9),
    createEggForTesting(withEggIdShell = shellOf("EggId5"), withPasswordShell = shellOf("Password5"), withSlot = S9),
)
