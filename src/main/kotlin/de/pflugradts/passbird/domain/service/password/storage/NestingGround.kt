package de.pflugradts.passbird.domain.service.password.storage

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.CURRENT_NEST
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.Companion.all
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.Companion.inNest
import java.util.Optional
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

@Singleton
class NestingGround @Inject constructor(
    @Inject private val passwordStoreAdapterPort: PasswordStoreAdapterPort,
    @Inject private val nestService: NestService,
    @Inject private val eventRegistry: EventRegistry,
) : EggRepository {
    private var lazyEggs: MutableList<Egg>? = null
    private val eggs: MutableList<Egg> get() {
        if (lazyEggs == null) {
            lazyEggs = passwordStoreAdapterPort.restore().get().toList().toMutableList()
            lazyEggs?.forEach {
                it.clearDomainEvents()
                eventRegistry.register(it)
            }
        }
        return lazyEggs!!
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
    override fun find(eggIdShell: Shell, nestSlot: NestSlot): Optional<Egg> = find(createEggStreamSupplier(nestSlot), eggIdShell)
    override fun find(eggIdShell: Shell): Optional<Egg> = find(createEggStreamSupplier(CURRENT_NEST), eggIdShell)
    private fun find(supplier: EggStreamSupplier, eggIdShell: Shell): Optional<Egg> =
        supplier.get().filter { it.viewEggId() == eggIdShell }.findAny()
    override fun findAll() = createEggStreamSupplier(CURRENT_NEST).get()
    private fun createEggStreamSupplier(eggFilter: EggFilter): EggStreamSupplier =
        createEggStreamSupplier(if (eggFilter == CURRENT_NEST) inNest(nestService.currentNest().nestSlot) else all())
    private fun createEggStreamSupplier(nestSlot: NestSlot) = createEggStreamSupplier(inNest(nestSlot))
    private fun createEggStreamSupplier(predicate: Predicate<Egg>) = Supplier { eggs.stream().filter(predicate) }
}

typealias EggStreamSupplier = Supplier<Stream<Egg>>
