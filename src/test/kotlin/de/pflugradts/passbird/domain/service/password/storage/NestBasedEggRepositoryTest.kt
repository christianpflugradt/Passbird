package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.createNestServiceSpyForTesting
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
import strikt.java.isPresent
import java.util.function.Supplier
import java.util.stream.Stream

class NestBasedEggRepositoryTest {

    private val givenEgg1 = createEggForTesting(withKeyBytes = bytesOf("key1"))
    private val givenEgg2 = createEggForTesting(withKeyBytes = bytesOf("key2"))
    private val givenEggs = listOf(givenEgg1, givenEgg2)

    private val passwordStoreAdapterPort = fakePasswordStoreAdapterPort(givenEggs)
    private val nestService = createNestServiceSpyForTesting()
    private val passbirdEventRegistry = mockk<PassbirdEventRegistry>(relaxed = true)
    private val repository = NestBasedEggRepository(passwordStoreAdapterPort, nestService, passbirdEventRegistry)

    @Test
    fun `should initialize`() {
        // given / when / then
        verify(exactly = 1) { passbirdEventRegistry.register(givenEgg1) }
        verify(exactly = 1) { passbirdEventRegistry.register(givenEgg2) }
    }

    @Test
    fun `should sync`() {
        // given
        val eggsSlot = slot<Supplier<Stream<Egg>>>()

        // when
        repository.sync()

        // then
        verify { passwordStoreAdapterPort.sync(capture(eggsSlot)) }
        expectThat(eggsSlot.captured.get().toList()).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
    }

    @Test
    fun `should find egg`() {
        // given / when
        val actual = repository.find(givenEgg1.viewKey())

        // then
        expectThat(actual).isPresent() isEqualTo givenEgg1
    }

    @Test
    fun `should return empty optional if requested egg does not exist`() {
        // given
        val nonExistingEgg = createEggForTesting(withKeyBytes = bytesOf("unknown"))

        // when
        val actual = repository.find(nonExistingEgg.viewKey())

        // then
        expectThat(actual.isEmpty).isTrue()
    }

    @Test
    fun `should add egg`() {
        // given
        val newEgg = createEggForTesting(withKeyBytes = bytesOf("new"))

        // when
        repository.add(newEgg)

        // then
        verify(exactly = 1) { passbirdEventRegistry.register(newEgg) }
        expectThat(repository.find(newEgg.viewKey())).isPresent() isEqualTo newEgg
    }

    @Test
    fun `should delete egg`() {
        // given / when
        repository.delete(givenEgg1)

        // then
        expectThat(repository.find(givenEgg1.viewKey()).isEmpty).isTrue()
    }

    @Test
    fun `should find all`() {
        // given / when
        val actual = repository.findAll()

        // then
        expectThat(actual.toList()).containsExactlyInAnyOrder(givenEgg1, givenEgg2)
    }

    @Nested
    inner class NestTest {
        @Test
        fun `should find all in current nest`() {
            // given
            val activeNestSlot = Slot.N2
            val otherNestSlot = Slot.N3
            nestService.deploy(bytesOf("nest"), activeNestSlot)
            nestService.deploy(bytesOf("nest"), otherNestSlot)
            val egg1 = createEggForTesting(withKeyBytes = bytesOf("first"), withNestSlot = activeNestSlot)
            val egg2 = createEggForTesting(withKeyBytes = bytesOf("second"), withNestSlot = activeNestSlot)
            val egg3 = createEggForTesting(withKeyBytes = bytesOf("third"), withNestSlot = otherNestSlot)
            repository.add(egg1)
            repository.add(egg2)
            repository.add(egg3)

            // when
            nestService.moveToNestAt(activeNestSlot)
            val actual = repository.findAll()

            // then
            expectThat(actual.toList()).containsExactlyInAnyOrder(egg1, egg2)
        }

        @Test
        fun `should store multiple ewith identical keys in different nests`() {
            // given
            val keyBytes = bytesOf("key")
            val firstNestSlot = Slot.N1
            val secondNestSlot = Slot.N2
            nestService.deploy(bytesOf("nest"), firstNestSlot)
            nestService.deploy(bytesOf("nest"), secondNestSlot)
            val egg1 = createEggForTesting(withKeyBytes = keyBytes, withNestSlot = firstNestSlot)
            val egg2 = createEggForTesting(withKeyBytes = keyBytes, withNestSlot = secondNestSlot)
            repository.add(egg1)
            repository.add(egg2)

            // when
            nestService.moveToNestAt(firstNestSlot)
            val actualFirstEgg = repository.find(keyBytes)
            nestService.moveToNestAt(secondNestSlot)
            val actualSecondEgg = repository.find(keyBytes)

            // then
            expectThat(actualFirstEgg).isPresent() isEqualTo egg1 isNotEqualTo egg2
            expectThat(actualSecondEgg).isPresent() isEqualTo egg2 isNotEqualTo egg1
        }
    }
}
