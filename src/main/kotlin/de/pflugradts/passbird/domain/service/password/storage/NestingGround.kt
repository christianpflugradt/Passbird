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

    override fun sync() { passwordStoreAdapterPort.sync(getEggsSupplier(EggFilter.ALL_NESTS)) }
    override fun find(eggIdShell: Shell, nestSlot: NestSlot): Optional<Egg> =
        find(getEggsSupplier(nestSlot), eggIdShell)
    override fun find(eggIdShell: Shell): Optional<Egg> =
        find(getEggsSupplier(EggFilter.CURRENT_NEST), eggIdShell)

    private fun find(supplier: Supplier<Stream<Egg>>, eggIdShell: Shell): Optional<Egg> =
        supplier.get().filter { it.viewEggId() == eggIdShell }.findAny()

    override fun add(egg: Egg) {
        eventRegistry.register(egg)
        eggs.add(egg)
    }

    override fun delete(egg: Egg) {
        eggs.remove(egg)
        eventRegistry.deregister(egg)
    }

    override fun findAll() = getEggsSupplier(EggFilter.CURRENT_NEST).get()

    private fun getEggsSupplier(eggFilter: EggFilter): Supplier<Stream<Egg>> =
        getEggsSupplier(
            if ((eggFilter == EggFilter.CURRENT_NEST)) {
                inNest(nestService.currentNest().nestSlot)
            } else {
                all()
            },
        )

    private fun getEggsSupplier(nestSlot: NestSlot) = getEggsSupplier(inNest(nestSlot))

    private fun getEggsSupplier(predicate: Predicate<Egg>) = Supplier { eggs.stream().filter(predicate) }
}