package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.ShellPairMap
import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.nest.NestSlot.N2
import de.pflugradts.passbird.domain.model.nest.NestSlot.N9
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.service.createNestServiceSpyForTesting
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.fakePasswordService
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
import java.util.stream.Stream

class PasswordImportExportServiceTest {

    private val exchangeFactory = mockk<ExchangeFactory>()
    private val passwordService = mockk<PasswordService>()
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val nestService = createNestServiceSpyForTesting()
    private val passbirdHomeUri = "any uri"
    private val importExportServiceSupplier get() =
        Supplier { PasswordImportExportService(exchangeFactory, passwordService, nestService, eventRegistry) }

    @BeforeEach
    fun setup() { mainMocked(arrayOf(passbirdHomeUri)) }

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
        val givenCurrentNestSlot = N2
        val eggs = testData()
        fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory, withEggs = eggs)
        fakePasswordService(instance = passwordService)
        nestService.place(shellOf("n2"), N2)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val importSlot = mutableListOf<Stream<ShellPair>>()
        val eggCountSlot = slot<EggsImported>()

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        verify { passwordService.putEggs(capture(importSlot)) }
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify(exactly = 1) { nestService.place(shellOf("Nest-9"), N9) }
        expectThat(importSlot) hasSize 3
        expectThatActualBytePairsMatchExpected(importSlot[0], eggs.subList(0, 2))
        expectThatActualBytePairsMatchExpected(importSlot[1], eggs.subList(2, 3))
        expectThatActualBytePairsMatchExpected(importSlot[2], eggs.subList(3, 5))
        expectThat(nestService.currentNest().nestSlot) isEqualTo givenCurrentNestSlot
        verify { eventRegistry.register(capture(eggCountSlot)) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(eggCountSlot.isCaptured)
        expectThat(eggCountSlot.captured.count) isEqualTo testData().size
    }

    @Test
    fun `should export passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = N2
        val eggs = testData()
        val exchangeAdapterPort = fakeExchangeAdapterPort(forExchangeFactory = exchangeFactory)
        fakePasswordService(instance = passwordService, withEggs = eggs, withNestService = nestService)
        nestService.place(shellOf("n2"), N2)
        nestService.place(shellOf("n2"), N9)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val exportNestSlot = slot<ShellPairMap>()
        val eggCountSlot = slot<EggsExported>()

        // when
        importExportServiceSupplier.get().exportEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify { exchangeAdapterPort.send(capture(exportNestSlot)) }
        val actual = exportNestSlot.captured
        expectThatActualBytePairsMatchExpected(actual, eggs)
        expectThat(actual) hasSize 3 containsKey DEFAULT containsKey N2 containsKey N9
        expectThat(nestService.currentNest().nestSlot) isEqualTo givenCurrentNestSlot
        verify { eventRegistry.register(capture(eggCountSlot)) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(eggCountSlot.isCaptured)
        expectThat(eggCountSlot.captured.count) isEqualTo testData().size
    }
}

private fun expectThatActualEggIdsMatchExpected(actual: ShellMap, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { nestSlot ->
        actual[nestSlot]!!.forEach {
            expectThat(it) isEqualTo expected[index++].viewEggId()
        }
    }
}
private fun expectThatActualBytePairsMatchExpected(actual: Stream<ShellPair>, expected: List<Egg>) {
    val actualList = actual.toList()
    expectThat(actualList.size) isEqualTo expected.size
    actualList.forEachIndexed { index, _ ->
        expectThat(actualList[index].first) isEqualTo expected[index].viewEggId()
        expectThat(actualList[index].second) isEqualTo expected[index].viewPassword()
    }
}

private fun expectThatActualBytePairsMatchExpected(actual: ShellPairMap, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { nestSlot ->
        actual[nestSlot]!!.forEach {
            expectThat(it.first) isEqualTo expected[index].viewEggId()
            expectThat(it.second) isEqualTo expected[index++].viewPassword()
        }
    }
}

private fun testData() = listOf(
    createEggForTesting(withEggIdShell = shellOf("EggId1"), withPasswordShell = shellOf("Password1"), withNestSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId2"), withPasswordShell = shellOf("Password2"), withNestSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId3"), withPasswordShell = shellOf("Password3"), withNestSlot = N2),
    createEggForTesting(withEggIdShell = shellOf("EggId4"), withPasswordShell = shellOf("Password4"), withNestSlot = N9),
    createEggForTesting(withEggIdShell = shellOf("EggId5"), withPasswordShell = shellOf("Password5"), withNestSlot = N9),
)
