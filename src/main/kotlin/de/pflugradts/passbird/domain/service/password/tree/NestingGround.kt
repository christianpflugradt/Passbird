package de.pflugradts.passbird.domain.service.password.tree

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggId
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import java.util.function.Predicate

@Singleton
class NestingGround @Inject constructor(
    private val passwordTreeAdapterPort: PasswordTreeAdapterPort,
    private val nestService: NestService,
    private val eventRegistry: EventRegistry,
) : EggRepository {
    private val lazyMemory: MutableOption<MemoryMap> = mutableOptionOf()
    private val lazyEggs: MutableOption<MutableList<Egg>> = mutableOptionOf()
    private val memory: MemoryMap get() = initializeIfEmpty().run { lazyMemory.get() }
    private val eggs: MutableList<Egg> get() = initializeIfEmpty().run { lazyEggs.get() }
    private val currentNestSlot get() = nestService.currentNest().slot

    private fun initializeIfEmpty() {
        if (lazyEggs.isEmpty) {
            val initialState = passwordTreeAdapterPort.restore()
            lazyEggs.set(initialState.get().toList().toMutableList())
            lazyMemory.set(initialState.memory())
            lazyEggs.get().forEach {
                it.clearDomainEvents()
                eventRegistry.register(it)
            }
        }
    }

    override fun add(egg: Egg) {
        eventRegistry.register(egg)
        eggs.add(egg)
    }

    override fun delete(egg: Egg) {
        eggs.remove(egg)
        eventRegistry.deregister(egg)
    }

    override fun sync() = passwordTreeAdapterPort.sync(EggStreamSupplier({ eggs.stream() }, memory))
    override fun findAll(slot: Slot) = createEggStreamSupplier(slot).get()
    override fun findAll() = createEggStreamSupplier(inNest(currentNestSlot)).get()
    private fun createEggStreamSupplier(slot: Slot) = createEggStreamSupplier(inNest(slot))
    private fun createEggStreamSupplier(predicate: Predicate<Egg>) = EggStreamSupplier({ eggs.stream().filter(predicate) })

    override fun memory() = memory[currentNestSlot]!!.copy()
    override fun updateMemory(mostRecentEggId: EggId) = with(memory[currentNestSlot]!!) {
        (size - 1 downTo 1).forEach { this[it].set(this[it - 1].get()) }
        this[0].set(mostRecentEggId.view())
    }
}

private fun inNest(slot: Slot) = Predicate<Egg> { it.associatedNest() == slot }
