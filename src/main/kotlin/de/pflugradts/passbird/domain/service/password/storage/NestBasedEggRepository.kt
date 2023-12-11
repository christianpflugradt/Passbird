package de.pflugradts.passbird.domain.service.password.storage

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.Companion.all
import de.pflugradts.passbird.domain.service.password.storage.EggFilter.Companion.inNest
import java.util.Optional
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

@Singleton
class NestBasedEggRepository @Inject constructor(
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
    override fun find(keyBytes: Bytes, nestSlot: Slot): Optional<Egg> =
        find(getEggsSupplier(nestSlot), keyBytes)
    override fun find(keyBytes: Bytes): Optional<Egg> =
        find(getEggsSupplier(EggFilter.CURRENT_NEST), keyBytes)

    private fun find(supplier: Supplier<Stream<Egg>>, keyBytes: Bytes): Optional<Egg> =
        supplier.get().filter { it.viewKey() == keyBytes }.findAny()

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
                inNest(nestService.getCurrentNest().slot)
            } else {
                all()
            },
        )

    private fun getEggsSupplier(nestSlot: Slot) = getEggsSupplier(inNest(nestSlot))

    private fun getEggsSupplier(predicate: Predicate<Egg>) = Supplier { eggs.stream().filter(predicate) }
}
