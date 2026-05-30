package de.pflugradts.passbird.application.exchange

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.adapter.exchange.FilePasswordExchange
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.application.PasswordInfoMap
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.fakeExchangeAdapterPort
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
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
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.assertions.map
import java.io.File
import java.nio.file.Files
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
    fun `should scramble parsed import shells after peeking egg ids`() {
        // given
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        val eggId = spyk(shellOf("EggId"))
        val password = spyk(shellOf("Password"))
        val proteinType = spyk(shellOf("ProteinType"))
        val proteinStructure = spyk(shellOf("ProteinStructure"))
        every { exchangeAdapterPort.receive() } returns success(
            mapOf(
                nestService.currentNest() to listOf(
                    Pair(
                        ShellPair(eggId, password),
                        proteinShellPairs(S3 to ShellPair(proteinType, proteinStructure)),
                    ),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort

        // when
        val actual = importExportServiceSupplier.get().peekImportEggIdShells()

        // then
        expectThat(actual.failure).isFalse()
        expectThat(actual.getOrNull()!![DEFAULT]!!.single()) isEqualTo shellOf("EggId")
        verify(exactly = 1) { eggId.scramble() }
        verify(exactly = 1) { password.scramble() }
        verify(exactly = 1) { proteinType.scramble() }
        verify(exactly = 1) { proteinStructure.scramble() }
    }

    @Test
    fun `should import passwords across multiple nests`() {
        // given
        val givenCurrentNestSlot = S2
        val eggs = testData()
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        every { exchangeAdapterPort.receive() } returns success(importDataMatchingExistingNests(eggs))
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        fakePasswordService(instance = passwordService)
        nestService.place(shellOf(S2.name), S2)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val putEggCalls = mutableListOf<PutEggCall>()
        passwordService.capturePutEggCalls(putEggCalls)
        val eggCountSlot = slot<EggsImported>()

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify(exactly = 1) { nestService.place(shellOf(S9.name), S9) }
        expectThat(putEggCalls) hasSize eggs.size
        eggs.indices.forEach { i ->
            expectThat(putEggCalls[i].eggIdShell) isEqualTo eggs[i].viewEggId().fakeDec()
            expectThat(putEggCalls[i].passwordShell) isEqualTo eggs[i].viewPassword().fakeDec()
        }
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
        verify { eventRegistry.register(capture(eggCountSlot)) }
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(eggCountSlot.isCaptured)
        expectThat(eggCountSlot.captured.count) isEqualTo testData().size
    }

    @Test
    fun `should scramble parsed import shells after importing`() {
        // given
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        val eggId = spyk(shellOf("EggId"))
        val password = spyk(shellOf("Password"))
        val proteinType = spyk(shellOf("ProteinType"))
        val proteinStructure = spyk(shellOf("ProteinStructure"))
        every { exchangeAdapterPort.receive() } returns success(
            mapOf(
                nestService.currentNest() to listOf(
                    Pair(
                        ShellPair(eggId, password),
                        proteinShellPairs(S3 to ShellPair(proteinType, proteinStructure)),
                    ),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        fakePasswordService(instance = passwordService)

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        verify(exactly = 1) { eggId.scramble() }
        verify(exactly = 1) { password.scramble() }
        verify(exactly = 1) { proteinType.scramble() }
        verify(exactly = 1) { proteinStructure.scramble() }
    }

    @Test
    fun `should not import passwords when a full import target slot contains a different nest`() {
        // given
        val givenCurrentNestSlot = S2
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        every { exchangeAdapterPort.receive() } returns success(
            linkedMapOf(
                createNest(shellOf("imported"), S2) to listOf(
                    Pair(ShellPair(shellOf("EggId"), shellOf("Password")), emptyList()),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        nestService.place(shellOf("local"), S2)
        nestService.moveToNestAt(givenCurrentNestSlot)
        val captureSystemErr = captureSystemErr()

        // when
        captureSystemErr.during {
            importExportServiceSupplier.get().importEggs()
        }

        // then
        verify { passwordService wasNot Called }
        verify(exactly = 0) { eventRegistry.register(any<DomainEvent>()) }
        verify(exactly = 0) { eventRegistry.processEvents() }
        expectThat(nestService.currentNest().slot) isEqualTo givenCurrentNestSlot
        expectThat(captureSystemErr.capture) isEqualTo "Password Tree could not be imported.\n"
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
        val putEggCalls = mutableListOf<PutEggCall>()
        val putProteinCalls = mutableListOf<PutProteinCall>()
        passwordService.capturePutEggCalls(putEggCalls)
        passwordService.capturePutProteinCalls(putProteinCalls)

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        expectThat(putEggCalls.single()) isEqualTo PutEggCall(shellOf("EggId"), shellOf("Password"))
        expectThat(putProteinCalls) hasSize 2
        expectThat(putProteinCalls[0]) isEqualTo PutProteinCall(shellOf("EggId"), Slot.S3, shellOf("type3"), shellOf("structure3"))
        expectThat(putProteinCalls[1]) isEqualTo PutProteinCall(shellOf("EggId"), S9, shellOf("type9"), shellOf("structure9"))
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
        val putEggCalls = mutableListOf<PutEggCall>()
        val putProteinCalls = mutableListOf<PutProteinCall>()
        passwordService.capturePutEggCalls(putEggCalls)
        passwordService.capturePutProteinCalls(putProteinCalls)
        val importedCount = slot<EggsImported>()

        // when
        importExportServiceSupplier.get().importEggs(S2, S9)

        // then
        expectThat(putEggCalls.single()) isEqualTo PutEggCall(shellOf("EggId"), shellOf("Password"))
        expectThat(putProteinCalls.single()) isEqualTo PutProteinCall(shellOf("EggId"), S3, shellOf("type3"), shellOf("structure3"))
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
    fun `should reject empty custom nest id before preview or import mutation`() {
        // given
        val tempExchangeDirectory = Files.createTempDirectory("passbird-import-empty-nest")
        writeExchangeFile(
            tempExchangeDirectory.toString(),
            """
            {
              "exportedContent": [
                {
                  "exportedNest": {
                    "nestId": "",
                    "slot": 2
                  },
                  "exportedEggs": [
                    {
                      "eggId": "EggId",
                      "password": "Password",
                      "proteins": []
                    }
                  ]
                }
              ]
            }
            """,
        )
        val importExportService = PasswordImportExportService(
            object : ExchangeFactory {
                override fun createPasswordExchange() = FilePasswordExchange(
                    SystemOperation(),
                    PassbirdRunContext(tempExchangeDirectory.toString().toDirectory(), DEFAULT),
                )
            },
            passwordService,
            nestService,
            eventRegistry,
        )

        try {
            // when
            val fullPreview = captureSystemErr().during { importExportService.peekImportEggIdShells() }
            val selectivePreview = captureSystemErr().during { importExportService.peekImportNests() }
            captureSystemErr().during { importExportService.importEggs() }

            // then
            expectThat(fullPreview.failure).isTrue()
            expectThat(selectivePreview.failure).isTrue()
            expectThat(nestService.atNestSlot(S2).isEmpty).isTrue()
            verify { passwordService wasNot Called }
            verify(exactly = 0) { eventRegistry.register(any<DomainEvent>()) }
            verify(exactly = 0) { eventRegistry.processEvents() }
        } finally {
            File(tempExchangeDirectory.toString()).deleteRecursively()
        }
    }

    @Test
    fun `should stop importing and suppress success events when putting an egg fails`() {
        // given
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        val firstEggId = shellOf("EggId1")
        val firstPassword = shellOf("Password1")
        val secondEggId = shellOf("EggId2")
        val secondPassword = shellOf("Password2")
        every { exchangeAdapterPort.receive() } returns success(
            mapOf(
                nestService.currentNest() to listOf(
                    Pair(ShellPair(firstEggId, firstPassword), emptyList()),
                    Pair(ShellPair(secondEggId, secondPassword), emptyList()),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        fakePasswordService(instance = passwordService)
        val putEggCalls = mutableListOf<PutEggCall>()
        passwordService.capturePutEggCalls(putEggCalls) {
            it == PutEggCall(shellOf("EggId1"), shellOf("Password1"))
        }

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        expectThat(putEggCalls.single()) isEqualTo PutEggCall(shellOf("EggId1"), shellOf("Password1"))
        verify(exactly = 0) { passwordService.putProtein(any(), any(), any(), any()) }
        verify(exactly = 0) { eventRegistry.register(any<DomainEvent>()) }
        verify(exactly = 0) { eventRegistry.processEvents() }
        verify(exactly = 1) { eventRegistry.clearEvents() }
    }

    @Test
    fun `should stop importing and suppress success events when putting a protein fails`() {
        // given
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        val firstEggId = shellOf("EggId1")
        val firstPassword = shellOf("Password1")
        val secondEggId = shellOf("EggId2")
        val secondPassword = shellOf("Password2")
        every { exchangeAdapterPort.receive() } returns success(
            mapOf(
                nestService.currentNest() to listOf(
                    Pair(
                        ShellPair(firstEggId, firstPassword),
                        proteinShellPairs(S3 to ShellPair(shellOf("type3"), shellOf("structure3"))),
                    ),
                    Pair(ShellPair(secondEggId, secondPassword), emptyList()),
                ),
            ),
        )
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        fakePasswordService(instance = passwordService)
        val putEggCalls = mutableListOf<PutEggCall>()
        val putProteinCalls = mutableListOf<PutProteinCall>()
        passwordService.capturePutEggCalls(putEggCalls)
        passwordService.capturePutProteinCalls(putProteinCalls) {
            it == PutProteinCall(shellOf("EggId1"), S3, shellOf("type3"), shellOf("structure3"))
        }

        // when
        importExportServiceSupplier.get().importEggs()

        // then
        expectThat(putEggCalls.single()) isEqualTo PutEggCall(shellOf("EggId1"), shellOf("Password1"))
        expectThat(putProteinCalls.single()) isEqualTo PutProteinCall(shellOf("EggId1"), S3, shellOf("type3"), shellOf("structure3"))
        verify(exactly = 0) { eventRegistry.register(any<DomainEvent>()) }
        verify(exactly = 0) { eventRegistry.processEvents() }
        verify(exactly = 1) { eventRegistry.clearEvents() }
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
        every { exchangeAdapterPort.send(capture(exportNestSlot)) } answers {
            expectThatActualBytePairsMatchExpected(exportNestSlot.captured, eggs)
            success(Unit)
        }

        // when
        importExportServiceSupplier.get().exportEggs()

        // then
        verify(exactly = 1) { exchangeFactory.createPasswordExchange() }
        verify { exchangeAdapterPort.send(any()) }
        val actual = exportNestSlot.captured
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
    fun `should scramble exported shells after exchange completes`() {
        // given
        val exportedEgg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withPasswordShell = shellOf("Password"),
            withProteins = mapOf(S3 to ShellPair(shellOf("ProteinType"), shellOf("ProteinStructure"))),
        )
        val exchangeAdapterPort = mockk<ExchangeAdapterPort>()
        val exportNestSlot = slot<PasswordInfoMap>()
        every { exchangeAdapterPort.send(capture(exportNestSlot)) } returns success(Unit)
        every { exchangeFactory.createPasswordExchange() } returns exchangeAdapterPort
        fakePasswordService(instance = passwordService, withEggs = listOf(exportedEgg), withNestService = nestService)

        // when
        importExportServiceSupplier.get().exportEggs(setOf(DEFAULT))

        // then
        val exportedPasswordInfo = exportNestSlot.captured.values.single().single()
        expectThat(exportedPasswordInfo.first.first) isNotEqualTo shellOf("EggId")
        expectThat(exportedPasswordInfo.first.second) isNotEqualTo shellOf("Password")
        expectThat(exportedPasswordInfo.second[S3.index()].first) isNotEqualTo shellOf("ProteinType")
        expectThat(exportedPasswordInfo.second[S3.index()].second) isNotEqualTo shellOf("ProteinStructure")
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

private data class PutEggCall(val eggIdShell: Shell, val passwordShell: Shell)

private data class PutProteinCall(val eggIdShell: Shell, val slot: Slot, val typeShell: Shell, val structureShell: Shell)

private fun PasswordService.capturePutEggCalls(calls: MutableList<PutEggCall>, fails: (PutEggCall) -> Boolean = { false }) {
    every { putEgg(any(), any()) } answers {
        val call = PutEggCall(firstArg<Shell>().copy(), secondArg<Shell>().copy())
        calls.add(call)
        if (fails(call)) failure(IllegalStateException("disk full")) else success(Unit)
    }
}

private fun PasswordService.capturePutProteinCalls(calls: MutableList<PutProteinCall>, fails: (PutProteinCall) -> Boolean = { false }) {
    every { putProtein(any(), any(), any(), any()) } answers {
        val call = PutProteinCall(
            eggIdShell = firstArg<Shell>().copy(),
            slot = secondArg(),
            typeShell = thirdArg<Shell>().copy(),
            structureShell = lastArg<Shell>().copy(),
        )
        calls.add(call)
        if (fails(call)) failure(IllegalStateException("disk full")) else success(Unit)
    }
}

private fun proteinShellPairs(vararg proteins: Pair<Slot, ShellPair>) = MutableList(Slot.entries.size) {
    ShellPair(emptyShell(), emptyShell())
}.apply {
    proteins.forEach { (slot, shells) -> this[slot.index()] = shells }
}

private fun importDataMatchingExistingNests(eggs: List<Egg>): PasswordInfoMap = eggs.groupBy { it.associatedNest() }
    .mapKeys { (slot, _) -> createNest(shellOf(if (slot == DEFAULT) "Default" else slot.name), slot) }
    .mapValues { (_, eggs) ->
        eggs.map {
            PasswordInfo(
                first = ShellPair(it.viewEggId().fakeDec(), it.viewPassword().fakeDec()),
                second = emptyList(),
            )
        }
    }

private fun testData() = listOf(
    createEggForTesting(withEggIdShell = shellOf("EggId1"), withPasswordShell = shellOf("Password1"), withSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId2"), withPasswordShell = shellOf("Password2"), withSlot = DEFAULT),
    createEggForTesting(withEggIdShell = shellOf("EggId3"), withPasswordShell = shellOf("Password3"), withSlot = S2),
    createEggForTesting(withEggIdShell = shellOf("EggId4"), withPasswordShell = shellOf("Password4"), withSlot = S9),
    createEggForTesting(withEggIdShell = shellOf("EggId5"), withPasswordShell = shellOf("Password5"), withSlot = S9),
)

private fun writeExchangeFile(directory: String, content: String) {
    File(directory + File.separator + ReadableConfiguration.EXCHANGE_FILENAME).writeText(content.trimIndent())
}
