package de.pflugradts.passbird.application.exchange

import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.PasswordInfoMap
import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S9
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.nest.createNestServiceSpyForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.map
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
        expectThat(actual.failure).isFalse()
        expectThatActualEggIdsMatchExpected(actual.getOrNull()!!, eggs)
        verify { passwordService wasNot Called }
    }

    @Test
    fun `should peek import nest previews`() {
        // given
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        every { exchangeAdapterPort.receive() } returns success(
            linkedMapOf(
                createNest(shellOf("Default"), DEFAULT) to listOf(
                    Pair(ShellPair(shellOf("EggId1"), shellOf("Password1")), emptyList()),
                ),
                createNest(shellOf("work"), S2) to listOf(
                    Pair(ShellPair(shellOf("EggId2"), shellOf("Password2")), emptyList()),
                    Pair(ShellPair(shellOf("EggId3"), shellOf("Password3")), emptyList()),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort

        // when
        val actual = importExportServiceSupplier.get().peekImportNests()

        // then
        expectThat(actual.failure).isFalse()
        expectThat(actual.getOrNull()!!.map { Pair(it.slot, it.nestId.asString()) }) isEqualTo listOf(
            Pair(DEFAULT, "Default"),
            Pair(S2, "work"),
        )
        expectThat(actual.getOrNull()!![1].eggIds.map { it.asString() }) isEqualTo listOf("EggId2", "EggId3")
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
            expectThat(eggIdSlot[i]) isEqualTo eggs[i].viewEggId().fakeDec()
            expectThat(passwordSlot[i]) isEqualTo eggs[i].viewPassword().fakeDec()
        }
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
        verify { eventRegistry.register(capture(eggCountSlot)) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(eggCountSlot.isCaptured)
        expectThat(eggCountSlot.captured.count) isEqualTo testData().size
    }

    @Test
    fun `should import proteins into their declared slots when imported data is sparse`() {
        // given
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        val givenEggId = shellOf("EggId")
        val givenPassword = shellOf("Password")
        every { exchangeAdapterPort.receive() } returns success(
            mapOf(
                nestService.currentNest() to listOf(
                    Pair(
                        ShellPair(givenEggId, givenPassword),
                        proteinShellPairs(
                            Slot.S3 to ShellPair(shellOf("type3"), shellOf("structure3")),
                            S9 to ShellPair(shellOf("type9"), shellOf("structure9")),
                        ),
                    ),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        fakePasswordService(instance = passwordService)

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify(exactly = 1) { passwordService.putEgg(givenEggId, givenPassword) }
        verify(exactly = 1) { passwordService.putProtein(givenEggId, Slot.S3, shellOf("type3"), shellOf("structure3")) }
        verify(exactly = 1) { passwordService.putProtein(givenEggId, S9, shellOf("type9"), shellOf("structure9")) }
        verify(exactly = 2) { passwordService.putProtein(any(), any(), any(), any()) }
    }

    @Test
    fun `should import one selected nest into a different target slot`() {
        // given
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        val givenEggId = shellOf("EggId")
        val givenPassword = shellOf("Password")
        every { exchangeAdapterPort.receive() } returns success(
            linkedMapOf(
                createNest(shellOf("work"), S2) to listOf(
                    Pair(
                        ShellPair(givenEggId, givenPassword),
                        proteinShellPairs(
                            S3 to ShellPair(shellOf("type3"), shellOf("structure3")),
                        ),
                    ),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        fakePasswordService(instance = passwordService)
        nestService.place(shellOf("current"), S3)
        nestService.moveToNestAt(S3)
        val importedCount = slot<EggsImported>()

        // when
        importExportServiceSupplier.get().importEggs(S2, S9)

        // then
        verify(exactly = 1) { passwordService.putEgg(givenEggId, givenPassword) }
        verify(exactly = 1) { passwordService.putProtein(givenEggId, S3, shellOf("type3"), shellOf("structure3")) }
        expectThat(nestService.atNestSlot(S9).get().viewNestId().asString()) isEqualTo "work"
        expectThat(nestService.currentNest().slot) isEqualTo S3
        verify { eventRegistry.register(capture(importedCount)) }
        expectThat(importedCount.captured.count) isEqualTo 1
    }

    @Test
    fun `should not import passwords or register success event if import exchange fails`() {
        // given
        val givenCurrentNestSlot = S2
        fakeExchangeAdapterPort(
            forExchangeFactory = exchangeFactory,
            withReceiveFailure = IllegalStateException("import failed"),
        )
        fakePasswordService(instance = passwordService)
        nestService.place(shellOf("n2"), S2)
        nestService.moveToNestAt(givenCurrentNestSlot)

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify { passwordService wasNot Called }
        verify(exactly = 0) { eventRegistry.register(any<DomainEvent>()) }
        verify(exactly = 0) { eventRegistry.processEvents() }
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
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

    @Test
    fun `should export selected nests only`() {
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
        importExportServiceSupplier.get().exportEggs(setOf(DEFAULT, S9))

        // then
        verify { exchangeAdapterPort.send(capture(exportNestSlot)) }
        expectThat(exportNestSlot.captured) hasSize 2 containsKey DEFAULT.toNest() containsKey S9.toNest()
        expectThat(exportNestSlot.captured.containsKey(S2.toNest())).isFalse()
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
        verify { eventRegistry.register(capture(eggCountSlot)) }
        expectThat(eggCountSlot.captured.count) isEqualTo 4
    }

    @Test
    fun `should not register success event if export exchange fails`() {
        // given
        val givenCurrentNestSlot = S2
        val eggs = testData()
        fakeExchangeAdapterPort(
            forExchangeFactory = exchangeFactory,
            withSendFailure = IllegalStateException("export failed"),
        )
        fakePasswordService(instance = passwordService, withEggs = eggs, withNestService = nestService)
        nestService.place(shellOf("n2"), S2)
        nestService.place(shellOf("n9"), S9)
        nestService.moveToNestAt(givenCurrentNestSlot)

        // when
        importExportServiceSupplier.get().exportEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify(exactly = 0) { eventRegistry.register(any<DomainEvent>()) }
        verify(exactly = 0) { eventRegistry.processEvents() }
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
    }

    private fun Slot.toNest() = nestService.atNestSlot(this).get()
}

private fun expectThatActualEggIdsMatchExpected(actual: ShellMap, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { nestSlot ->
        actual[nestSlot]!!.forEach {
            expectThat(it) isEqualTo expected[index++].viewEggId().fakeDec()
        }
    }
}

private fun expectThatActualBytePairsMatchExpected(actual: PasswordInfoMap, expected: List<Egg>) {
    var index = 0
    actual.keys.forEach { nestSlot ->
        actual[nestSlot]!!.forEach {
            expectThat(it.first.first) isEqualTo expected[index].viewEggId().fakeDec()
            expectThat(it.first.second) isEqualTo expected[index++].viewPassword().fakeDec()
        }
    }
}

private fun proteinShellPairs(vararg proteins: Pair<Slot, ShellPair>) = MutableList(Slot.entries.size) {
    ShellPair(emptyShell(), emptyShell())
}.apply {
    proteins.forEach { (slot, shells) -> this[slot.index()] = shells }
}

private fun testData() = listOf(
    createEggForTesting(withEggIdShell = shellOf("EggId1"), withPasswordShell = shellOf("Password1"), withSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId2"), withPasswordShell = shellOf("Password2"), withSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId3"), withPasswordShell = shellOf("Password3"), withSlot = S2),
    createEggForTesting(withEggIdShell = shellOf("EggId4"), withPasswordShell = shellOf("Password4"), withSlot = S9),
    createEggForTesting(withEggIdShell = shellOf("EggId5"), withPasswordShell = shellOf("Password5"), withSlot = S9),
)
