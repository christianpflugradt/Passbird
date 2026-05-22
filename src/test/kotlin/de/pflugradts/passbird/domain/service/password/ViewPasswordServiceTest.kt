package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.application.security.fakeCryptoProvider
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.fakeEggRepository
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class ViewPasswordServiceTest {

    private val cryptoProvider = mockk<CryptoProvider>()
    private val eggRepository = mockk<EggRepository>()
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val passwordService = ViewPasswordService(cryptoProvider, eggRepository, eventRegistry)

    @Test
    fun `should return true if egg exists`() {
        // given
        val givenEggId = shellOf("EggId")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.eggExists(givenEggId, EggNotExistsAction.DO_NOTHING)

        // then
        verify { eventRegistry wasNot Called }
        expectThat(actual).isTrue()
    }

    @Test
    fun `should return false if egg does not exist`() {
        // given
        val givenEggId = shellOf("EggId")
        val otherEggId = shellOf("try this")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))

        // when
        val actual = passwordService.eggExists(otherEggId, EggNotExistsAction.DO_NOTHING)

        // then
        verify { eventRegistry wasNot Called }
        expectThat(actual).isFalse()
    }

    @Test
    fun `should find existing password`() {
        // given
        val givenEggId = shellOf("EggId")
        val expectedPassword = shellOf("Password")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId, withPasswordShell = expectedPassword)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))
        val encryptedShellSlot = mutableListOf<EncryptedShell>()

        // when
        val actual = passwordService.viewPassword(givenEggId)

        // then
        verify { cryptoProvider.decrypt(capture(encryptedShellSlot)) }
        expectThat(encryptedShellSlot.size) isEqualTo 2
        expectThat(encryptedShellSlot[0].fakeDec()) isEqualTo givenEggId
        expectThat(encryptedShellSlot[1].fakeDec()) isEqualTo expectedPassword
        verify { eventRegistry wasNot Called }
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get()) isEqualTo expectedPassword
    }

    @Test
    fun `should return empty optional if egg does not exist`() {
        // given
        val givenEggId = shellOf("EggId")
        val otherEggId = shellOf("tryThis")
        val matchingEgg = createEggForTesting(withEggIdShell = givenEggId)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(instance = eggRepository, withEggs = listOf(matchingEgg))
        val eggNotFoundSlot = slot<EggNotFound>()

        // when
        val actual = passwordService.viewPassword(otherEggId)

        // then
        verify { eventRegistry.register(capture(eggNotFoundSlot)) }
        expectThat(eggNotFoundSlot.isCaptured).isTrue()
        expectThat(eggNotFoundSlot.captured.eggIdShell) isEqualTo otherEggId
        verify(exactly = 1) { eventRegistry.processEvents() }
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should find all eggIds in alphabetical order`() {
        // given
        val eggId1 = shellOf("abc")
        val eggId2 = shellOf("hij")
        val eggId3 = shellOf("xyz")
        val egg1 = createEggForTesting(withEggIdShell = eggId1)
        val egg2 = createEggForTesting(withEggIdShell = eggId2)
        val egg3 = createEggForTesting(withEggIdShell = eggId3)
        fakeCryptoProvider(instance = cryptoProvider)
        fakeEggRepository(
            instance = eggRepository,
            withEggs = listOf(egg1, egg2, egg3),
        )

        // when
        val actual = passwordService.findAllEggIds()

        // then
        expectThat(actual.toList()).containsExactly(eggId1, eggId2, eggId3)
    }

    @Nested
    inner class ProteinTests {
        @Test
        fun `should report proteinExists true when protein present`() {
            // given
            val eggId = shellOf("eggA")
            val egg = createEggForTesting(withEggIdShell = eggId)
            fakeCryptoProvider(instance = cryptoProvider)
            val type = shellOf("type1")
            val structure = shellOf("struct1")
            val encType: EncryptedShell = cryptoProvider.encrypt(type)
            val encStructure: EncryptedShell = cryptoProvider.encrypt(structure)
            egg.updateProtein(Slot.S1, encType, encStructure)
            fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))

            // when
            val actual = passwordService.proteinExists(eggId, Slot.S1)

            // then
            expectThat(actual).isTrue()
            verify { eventRegistry wasNot Called }
        }

        @Test
        fun `should report proteinExists false when protein absent`() {
            // given
            val eggId = shellOf("eggB")
            val egg = createEggForTesting(withEggIdShell = eggId)
            fakeCryptoProvider(instance = cryptoProvider)
            fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))

            // when
            val actual = passwordService.proteinExists(eggId, Slot.S2)

            // then
            expectThat(actual).isFalse()
            verify { eventRegistry wasNot Called }
        }

        @Test
        fun `should report proteinExists false and raise event when egg missing`() {
            // given
            val missingEggId = shellOf("eggMissing")
            fakeCryptoProvider(instance = cryptoProvider)
            fakeEggRepository(instance = eggRepository)
            val eggNotFound = slot<EggNotFound>()

            // when
            val actual = passwordService.proteinExists(missingEggId, Slot.S3)

            // then
            expectThat(actual).isFalse()
            verify { eventRegistry.register(capture(eggNotFound)) }
            expectThat(eggNotFound.captured.eggIdShell) isEqualTo missingEggId
            verify(exactly = 1) { eventRegistry.processEvents() }
        }

        @Test
        fun `should view protein structure present`() {
            // given
            val eggId = shellOf("eggC")
            val egg = createEggForTesting(withEggIdShell = eggId)
            fakeCryptoProvider(instance = cryptoProvider)
            val type = shellOf("t")
            val struct = shellOf("structureC")
            egg.updateProtein(Slot.S1, cryptoProvider.encrypt(type), cryptoProvider.encrypt(struct))
            fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))

            // when
            val actual = passwordService.viewProteinStructure(eggId, Slot.S1)

            // then
            expectThat(actual.isPresent).isTrue()
            expectThat(actual.get()) isEqualTo struct
        }

        @Test
        fun `should view protein structure empty shell when protein absent`() {
            // given
            val eggId = shellOf("eggD")
            val egg = createEggForTesting(withEggIdShell = eggId)
            fakeCryptoProvider(instance = cryptoProvider)
            fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))

            // when
            val actual = passwordService.viewProteinStructure(eggId, Slot.S4)

            // then
            expectThat(actual.isPresent).isTrue()
            expectThat(actual.get().isEmpty).isTrue()
        }

        @Test
        fun `should return empty option when viewing protein structure on missing egg`() {
            // given
            val eggId = shellOf("eggE")
            fakeCryptoProvider(instance = cryptoProvider)
            fakeEggRepository(instance = eggRepository)
            val eggNotFoundSlot = slot<EggNotFound>()

            // when
            val actual = passwordService.viewProteinStructure(eggId, Slot.S5)

            // then
            expectThat(actual.isEmpty).isTrue()
            verify { eventRegistry.register(capture(eggNotFoundSlot)) }
            expectThat(eggNotFoundSlot.captured.eggIdShell) isEqualTo eggId
        }

        @Test
        fun `should view protein types and structures list`() {
            // given
            val eggId = shellOf("eggF")
            val egg = createEggForTesting(withEggIdShell = eggId)
            fakeCryptoProvider(instance = cryptoProvider)
            egg.updateProtein(Slot.S1, cryptoProvider.encrypt(shellOf("type1")), cryptoProvider.encrypt(shellOf("struct1")))
            egg.updateProtein(Slot.S3, cryptoProvider.encrypt(shellOf("type3")), cryptoProvider.encrypt(shellOf("struct3")))
            fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))

            // when
            val types = passwordService.viewProteinTypes(eggId)
            val structures = passwordService.viewProteinStructures(eggId)

            // then
            expectThat(types.isPresent).isTrue()
            expectThat(structures.isPresent).isTrue()
            val typeList = types.get()
            val structList = structures.get()
            expectThat(typeList[Slot.S1.index()].isPresent).isTrue()
            expectThat(typeList[Slot.S3.index()].isPresent).isTrue()
            expectThat(structList[Slot.S1.index()].isPresent).isTrue()
            expectThat(structList[Slot.S3.index()].isPresent).isTrue()
        }

        @Test
        fun `should return empty option for types list when egg missing`() {
            // given
            val eggId = shellOf("eggG")
            fakeCryptoProvider(instance = cryptoProvider)
            fakeEggRepository(instance = eggRepository)

            // when
            val types = passwordService.viewProteinTypes(eggId)

            // then
            expectThat(types.isEmpty).isTrue()
            verify(exactly = 1) { eventRegistry.processEvents() }
        }

        @Test
        fun `should view protein type present`() {
            // given
            val eggId = shellOf("eggTypeH")
            val egg = createEggForTesting(withEggIdShell = eggId)
            fakeCryptoProvider(instance = cryptoProvider)
            val type = shellOf("typeH")
            val structure = shellOf("structH")
            egg.updateProtein(Slot.S2, cryptoProvider.encrypt(type), cryptoProvider.encrypt(structure))
            fakeEggRepository(instance = eggRepository, withEggs = listOf(egg))

            // when
            val actual = passwordService.viewProteinType(eggId, Slot.S2)

            // then
            expectThat(actual.isPresent).isTrue()
            expectThat(actual.get()) isEqualTo type
            verify { eventRegistry wasNot Called }
        }
    }

    @Nested
    inner class MemoryAndExistenceTests {
        @Test
        fun `should raise event when eggExists called with create event action and egg missing`() {
            // given
            val eggId = shellOf("notThere")
            fakeCryptoProvider(instance = cryptoProvider)
            fakeEggRepository(instance = eggRepository)
            val nf = slot<EggNotFound>()

            // when
            val exists = passwordService.eggExists(eggId, EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT)

            // then
            expectThat(exists).isFalse()
            verify { eventRegistry.register(capture(nf)) }
            expectThat(nf.captured.eggIdShell) isEqualTo eggId
            verify(exactly = 1) { eventRegistry.processEvents() }
        }

        @Test
        fun `should return empty memory when no lookups performed`() {
            // given
            fakeCryptoProvider(instance = cryptoProvider)
            fakeEggRepository(instance = eggRepository)

            // when
            val memory = passwordService.viewMemory()
            val entry = passwordService.viewMemoryEntry(Slot.DEFAULT)

            // then
            memory.forEach { expectThat(it.isPresent).isFalse() }
            expectThat(entry.isEmpty).isTrue()
        }

        @Test
        fun `should memorize eggId after viewing password`() {
            // given
            val eggId = shellOf("memEgg")
            val password = shellOf("pw")
            val egg = createEggForTesting(withEggIdShell = eggId, withPasswordShell = password)
            fakeCryptoProvider(instance = cryptoProvider)
            val memory = EggIdMemory()
            every { eggRepository.findAll() } answers { listOf(egg).stream() }
            every { eggRepository.findAll(any<Slot>()) } answers { emptyList<Egg>().stream() }
            every { eggRepository.memory() } returns memory
            every { eggRepository.updateMemory(any(), any()) } answers {
                val updatedEgg = firstArg<Egg>()
                memory.memorize(updatedEgg.viewEggId(), null)
            }
            every { eggRepository.sync() } returns Unit

            // when
            passwordService.viewPassword(eggId)
            val memorySlots = passwordService.viewMemory()
            val entry = passwordService.viewMemoryEntry(Slot.DEFAULT)

            // then
            val firstSlot = memorySlots.first()
            expectThat(firstSlot.isPresent).isTrue()
            expectThat(firstSlot.get()) isEqualTo eggId
            expectThat(entry.isPresent).isTrue()
            expectThat(entry.get()) isEqualTo eggId
        }
    }
}
