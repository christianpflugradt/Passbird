package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.createNestServiceSpyForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class NestingGroundTest {

    private val givenEgg1 = createEggForTesting(withEggIdShell = shellOf("EggId1"))
    private val givenEgg2 = createEggForTesting(withEggIdShell = shellOf("EggId2"))
    private val givenEggs = listOf(givenEgg1, givenEgg2)

    private val passwordTreeAdapterPort = fakePasswordTreeAdapterPort(givenEggs)
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val nestService = createNestServiceSpyForTesting()
    private val nestingGround = NestingGround(passwordTreeAdapterPort, nestService, eventRegistry)

    @Test
    fun `should initialize`() {
        // given / when / then
        nestingGround.find(emptyShell())
        verify(exactly = 1) { eventRegistry.register(givenEgg1) }
        verify(exactly = 1) { eventRegistry.register(givenEgg2) }
    }

    @Test
    fun `should sync`() {
        // given
        val eggsSlot = slot<EggStreamSupplier>()

        // when
        nestingGround.sync()

        // then
        verify { passwordTreeAdapterPort.sync(capture(eggsSlot)) }
        expectThat(eggsSlot.captured.get().toList()).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
    }

    @Test
    fun `should find egg`() {
        // given / when
        val actual = nestingGround.find(givenEgg1.viewEggId().fakeDec())

        // then
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get()) isEqualTo givenEgg1
    }

    @Test
    fun `should return empty optional if requested egg does not exist`() {
        // given
        val nonExistingEgg = createEggForTesting(withEggIdShell = shellOf("unknown"))

        // when
        val actual = nestingGround.find(nonExistingEgg.viewEggId().fakeDec())

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
        nestingGround.find(newEgg.viewEggId().fakeDec()).let {
            expectThat(it.isPresent).isTrue()
            expectThat(it.get()) isEqualTo newEgg
        }
    }

    @Test
    fun `should delete egg`() {
        // given / when
        nestingGround.delete(givenEgg1)

        // then
        expectThat(nestingGround.find(givenEgg1.viewEggId().fakeDec()).isEmpty).isTrue()
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
            val actualFirstEgg = nestingGround.find(eggIdShells)
            nestService.moveToNestAt(secondSlot)
            val actualSecondEgg = nestingGround.find(eggIdShells)

            // then
            expectThat(actualFirstEgg.isPresent).isTrue()
            expectThat(actualFirstEgg.get()) isEqualTo egg1 isNotEqualTo egg2
            expectThat(actualSecondEgg.isPresent).isTrue()
            expectThat(actualSecondEgg.get()) isEqualTo egg2 isNotEqualTo egg1
        }
    }
}
