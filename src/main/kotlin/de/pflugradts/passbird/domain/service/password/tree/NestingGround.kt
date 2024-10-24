package de.pflugradts.passbird.domain.service.password.tree

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.tree.EggFilter.CURRENT_NEST
import de.pflugradts.passbird.domain.service.password.tree.EggFilter.Companion.all
import de.pflugradts.passbird.domain.service.password.tree.EggFilter.Companion.inNest
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

@Singleton
class NestingGround @Inject constructor(
    private val passwordTreeAdapterPort: PasswordTreeAdapterPort,
    private val nestService: NestService,
    private val eventRegistry: EventRegistry,
) : EggRepository {
    private val lazyEggs: MutableOption<MutableList<Egg>> = mutableOptionOf()
    private val eggs: MutableList<Egg> get() {
        if (lazyEggs.isEmpty) {
            lazyEggs.set(passwordTreeAdapterPort.restore().get().toList().toMutableList())
            lazyEggs.get().forEach {
                it.clearDomainEvents()
                eventRegistry.register(it)
            }
        }
        return lazyEggs.get()
    }

    override fun add(egg: Egg) {
        eventRegistry.register(egg)
        eggs.add(egg)
    }

    override fun delete(egg: Egg) {
        eggs.remove(egg)
        eventRegistry.deregister(egg)
    }

    override fun sync() = passwordTreeAdapterPort.sync(createEggStreamSupplier(EggFilter.ALL_NESTS))
    override fun findAll(slot: Slot) = createEggStreamSupplier(slot).get()
    override fun findAll() = createEggStreamSupplier(CURRENT_NEST).get()
    private fun createEggStreamSupplier(eggFilter: EggFilter): EggStreamSupplier =
        createEggStreamSupplier(if (eggFilter == CURRENT_NEST) inNest(nestService.currentNest().slot) else all())
    private fun createEggStreamSupplier(slot: Slot) = createEggStreamSupplier(inNest(slot))
    private fun createEggStreamSupplier(predicate: Predicate<Egg>) = Supplier { eggs.stream().filter(predicate) }
}

typealias EggStreamSupplier = Supplier<Stream<Egg>>
