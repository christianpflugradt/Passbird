package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.createNestServiceSpyForTesting
import de.pflugradts.passbird.domain.service.nest.findForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.io.IOException

class NestingGroundTest {

    private val givenEgg1 = createEggForTesting(withEggIdShell = shellOf("EggId1"))
    private val givenEgg2 = createEggForTesting(withEggIdShell = shellOf("EggId2"))
    private val givenEggs = listOf(givenEgg1, givenEgg2)

    private val configuration = mockk<Configuration>()
    private val passwordTreeAdapterPort = fakePasswordTreeAdapterPort(givenEggs)
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val nestService = createNestServiceSpyForTesting()
    private val nestingGround = NestingGround(eggIdMemoryEnabled = false, passwordTreeAdapterPort, nestService, eventRegistry)

    @BeforeEach
    fun setUp() {
        fakeConfiguration(instance = configuration)
    }

    @Test
    fun `should initialize upon first invocation`() {
        // given / when / then
        nestingGround.findForTesting(emptyShell())
        verify(exactly = 1) { eventRegistry.register(givenEgg1) }
        verify(exactly = 1) { eventRegistry.register(givenEgg2) }
    }

    @Test
    fun `should sync`() {
        // given
        val eggsSlot = slot<EggStreamSupplier>()
        val nestShell = shellOf("Nest")
        nestService.place(nestShell, Slot.S2)

        // when
        nestingGround.sync()

        // then
        verify { passwordTreeAdapterPort.sync(capture(eggsSlot)) }
        expectThat(eggsSlot.captured.get().toList()).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
        expectThat(eggsSlot.captured.nests()[1]) isEqualTo nestShell
    }

    @Test
    fun `should clear favorites for discarded nests when syncing`() {
        // given
        val eggsSlot = slot<EggStreamSupplier>()
        nestService.place(shellOf("Nest"), Slot.S1)
        nestService.moveToNestAt(Slot.S1)
        nestingGround.putFavorite(Slot.S1, givenEgg1.viewEggId())

        // when
        nestService.discardNestAt(Slot.S1)
        nestingGround.sync()

        // then
        verify { passwordTreeAdapterPort.sync(capture(eggsSlot)) }
        expectThat(eggsSlot.captured.favorites()[Slot.S1].get()[Slot.S1].isEmpty).isTrue()
    }

    @Test
    fun `should roll back added egg when sync fails`() {
        // given
        val failingNestingGround = failingNestingGround()
        val newEgg = createEggForTesting(withEggIdShell = shellOf("new"))

        // when
        failingNestingGround.add(newEgg)
        val actual = failingNestingGround.sync()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull()
        expectThat(failingNestingGround.findForTesting(newEgg.viewEggId().fakeDec()).isEmpty).isTrue()
    }

    @Test
    fun `should roll back password update when sync fails`() {
        // given
        val failingNestingGround = failingNestingGround()
        val updatedPassword = shellOf("updated")
        val originalPassword = givenEgg1.viewPassword().fakeDec()
        val egg = failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get()

        // when
        egg.updatePassword(updatedPassword.fakeEnc())
        val actual = failingNestingGround.sync()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get().viewPassword().fakeDec()) isEqualTo
            originalPassword
    }

    @Test
    fun `should roll back protein update when sync fails`() {
        // given
        val failingNestingGround = failingNestingGround()
        val egg = failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get()

        // when
        egg.updateProtein(Slot.S1, shellOf("type").fakeEnc(), shellOf("structure").fakeEnc())
        val actual = failingNestingGround.sync()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get().proteins[Slot.S1.index()].isEmpty).isTrue()
    }

    @Test
    fun `should roll back rename when sync fails`() {
        // given
        val failingNestingGround = failingNestingGround()
        val oldEggId = givenEgg1.viewEggId().fakeDec()
        val newEggId = shellOf("Renamed")
        val egg = failingNestingGround.findForTesting(oldEggId).get()

        // when
        egg.rename(newEggId.fakeEnc())
        val actual = failingNestingGround.sync()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(failingNestingGround.findForTesting(oldEggId).isPresent).isTrue()
        expectThat(failingNestingGround.findForTesting(newEggId).isEmpty).isTrue()
    }

    @Test
    fun `should roll back move when sync fails`() {
        // given
        val failingNestingGround = failingNestingGround()
        val egg = failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get()

        // when
        egg.moveToNestAt(Slot.S2)
        val actual = failingNestingGround.sync()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get().associatedNest()) isEqualTo Slot.DEFAULT
    }

    @Test
    fun `should roll back discard when sync fails`() {
        // given
        val failingNestingGround = failingNestingGround()
        val originalPassword = givenEgg1.viewPassword().fakeDec()
        val egg = failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get()

        // when
        egg.discard()
        val actual = failingNestingGround.sync()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(failingNestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).get().viewPassword().fakeDec()) isEqualTo
            originalPassword
    }

    @Test
    fun `should roll back favorite update when sync fails`() {
        // given
        val failingNestingGround = failingNestingGround()

        // when
        failingNestingGround.putFavorite(Slot.S1, givenEgg1.viewEggId())
        val actual = failingNestingGround.sync()

        // then
        expectThat(actual.failure).isTrue()
        expectThat(failingNestingGround.favorites()[Slot.S1].isEmpty).isTrue()
    }

    @Test
    fun `should find egg`() {
        // given / when
        val actual = nestingGround.findForTesting(givenEgg1.viewEggId().fakeDec())

        // then
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get()) isEqualTo givenEgg1
    }

    @Test
    fun `should return empty optional if requested egg does not exist`() {
        // given
        val nonExistingEgg = createEggForTesting(withEggIdShell = shellOf("unknown"))

        // when
        val actual = nestingGround.findForTesting(nonExistingEgg.viewEggId().fakeDec())

        // then
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should add egg`() {
        // given
        val newEgg = createEggForTesting(withEggIdShell = shellOf("new"))

        // when
        nestingGround.add(newEgg)

        // then
        verify(exactly = 1) { eventRegistry.register(newEgg) }
        nestingGround.findForTesting(newEgg.viewEggId().fakeDec()).let {
            expectThat(it.isPresent).isTrue()
            expectThat(it.get()) isEqualTo newEgg
        }
    }

    @Test
    fun `should delete egg`() {
        // given / when
        nestingGround.delete(givenEgg1)

        // then
        expectThat(nestingGround.findForTesting(givenEgg1.viewEggId().fakeDec()).isEmpty).isTrue()
    }

    @Test
    fun `should find all`() {
        // given / when
        val actual = nestingGround.findAll()

        // then
        expectThat(actual.toList()).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
    }

    @Nested
    inner class NestTest {
        @Test
        fun `should find all in current nest`() {
            // given
            val activeSlot = Slot.S2
            val otherSlot = Slot.S3
            nestService.place(shellOf("Nest"), activeSlot)
            nestService.place(shellOf("Nest"), otherSlot)
            val egg1 = createEggForTesting(withEggIdShell = shellOf("first"), withSlot = activeSlot)
            val egg2 = createEggForTesting(withEggIdShell = shellOf("second"), withSlot = activeSlot)
            val egg3 = createEggForTesting(withEggIdShell = shellOf("third"), withSlot = otherSlot)
            nestingGround.add(egg1)
            nestingGround.add(egg2)
            nestingGround.add(egg3)

            // when
            nestService.moveToNestAt(activeSlot)
            val actual = nestingGround.findAll()

            // then
            expectThat(actual.toList()).containsExactlyInAnyOrder(egg1, egg2)
        }

        @Test
        fun `should store multiple eggs with identical eggIds in different nests`() {
            // given
            val eggIdShells = shellOf("EggId")
            val firstSlot = Slot.S1
            val secondSlot = Slot.S2
            nestService.place(shellOf("Nest"), firstSlot)
            nestService.place(shellOf("Nest"), secondSlot)
            val egg1 = createEggForTesting(withEggIdShell = eggIdShells, withSlot = firstSlot)
            val egg2 = createEggForTesting(withEggIdShell = eggIdShells, withSlot = secondSlot)
            nestingGround.add(egg1)
            nestingGround.add(egg2)

            // when
            nestService.moveToNestAt(firstSlot)
            val actualFirstEgg = nestingGround.findForTesting(eggIdShells)
            nestService.moveToNestAt(secondSlot)
            val actualSecondEgg = nestingGround.findForTesting(eggIdShells)

            // then
            expectThat(actualFirstEgg.isPresent).isTrue()
            expectThat(actualFirstEgg.get()) isEqualTo egg1 isNotEqualTo egg2
            expectThat(actualSecondEgg.isPresent).isTrue()
            expectThat(actualSecondEgg.get()) isEqualTo egg2 isNotEqualTo egg1
        }
    }

    private fun failingNestingGround() = NestingGround(
        eggIdMemoryEnabled = false,
        passwordTreeAdapterPort = fakePasswordTreeAdapterPort(givenEggs, withSyncFailure = IOException("disk full")),
        nestStateView = nestService,
        eventRegistry = eventRegistry,
    )
}
