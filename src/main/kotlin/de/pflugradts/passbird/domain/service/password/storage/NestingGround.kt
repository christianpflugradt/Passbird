package de.pflugradts.passbird.domain.service.password.storage

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.toOption
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.CURRENT_NEST
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.Companion.all
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.Companion.inNest
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

@Singleton
class NestingGround @Inject constructor(
    @Inject private val passwordStoreAdapterPort: PasswordStoreAdapterPort,
    @Inject private val nestService: NestService,
    @Inject private val eventRegistry: EventRegistry,
) : EggRepository {
    private val lazyEggs: MutableOption<MutableList<Egg>> = mutableOptionOf()
    private val eggs: MutableList<Egg> get() {
        if (lazyEggs.isEmpty) {
            lazyEggs.set(passwordStoreAdapterPort.restore().get().toList().toMutableList())
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

    override fun sync() { passwordStoreAdapterPort.sync(createEggStreamSupplier(EggFilter.ALL_NESTS)) }
    override fun find(eggIdShell: Shell, slot: Slot): Option<Egg> = find(createEggStreamSupplier(slot), eggIdShell)
    override fun find(eggIdShell: Shell): Option<Egg> = find(createEggStreamSupplier(CURRENT_NEST), eggIdShell)
    private fun find(supplier: EggStreamSupplier, eggIdShell: Shell): Option<Egg> =
        supplier.get().filter { it.viewEggId() == eggIdShell }.findAny().toOption()
    override fun findAll() = createEggStreamSupplier(CURRENT_NEST).get()
    private fun createEggStreamSupplier(eggFilter: EggFilter): EggStreamSupplier =
        createEggStreamSupplier(if (eggFilter == CURRENT_NEST) inNest(nestService.currentNest().slot) else all())
    private fun createEggStreamSupplier(slot: Slot) = createEggStreamSupplier(inNest(slot))
    private fun createEggStreamSupplier(predicate: Predicate<Egg>) = Supplier { eggs.stream().filter(predicate) }
}

typealias EggStreamSupplier = Supplier<Stream<Egg>>
