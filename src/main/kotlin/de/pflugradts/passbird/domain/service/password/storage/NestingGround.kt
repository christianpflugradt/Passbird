package de.pflugradts.passbird.domain.service.password.storage

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
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

    private val eggs = passwordStoreAdapterPort.restore().get().toList().toMutableList()

    init {
        eggs.forEach {
            it.clearDomainEvents()
            eventRegistry.register(it)
        }
    }

    override fun sync() { passwordStoreAdapterPort.sync(createEggStreamSupplier(EggFilter.ALL_NESTS)) }
    override fun find(eggIdShell: Shell, nestSlot: NestSlot): Optional<Egg> =
        find(createEggStreamSupplier(nestSlot), eggIdShell)
    override fun find(eggIdShell: Shell): Optional<Egg> =
        find(createEggStreamSupplier(EggFilter.CURRENT_NEST), eggIdShell)

    private fun find(supplier: EggStreamSupplier, eggIdShell: Shell): Optional<Egg> =
        supplier.get().filter { it.viewEggId() == eggIdShell }.findAny()

    override fun add(egg: Egg) {
        eventRegistry.register(egg)
        eggs.add(egg)
    }

    override fun delete(egg: Egg) {
        eggs.remove(egg)
        eventRegistry.deregister(egg)
    }

    override fun findAll() = createEggStreamSupplier(EggFilter.CURRENT_NEST).get()

    private fun createEggStreamSupplier(eggFilter: EggFilter): EggStreamSupplier =
        createEggStreamSupplier(
            if ((eggFilter == EggFilter.CURRENT_NEST)) {
                inNest(nestService.currentNest().nestSlot)
            } else {
                all()
            },
        )

    private fun createEggStreamSupplier(nestSlot: NestSlot) = createEggStreamSupplier(inNest(nestSlot))

    private fun createEggStreamSupplier(predicate: Predicate<Egg>) = Supplier { eggs.stream().filter(predicate) }
}

typealias EggStreamSupplier = Supplier<Stream<Egg>>
