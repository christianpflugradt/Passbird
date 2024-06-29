package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService

class PasswordImportExportService @Inject constructor(
    @Inject private val exchangeFactory: ExchangeFactory,
    @Inject private val passwordService: PasswordService,
    @Inject private val nestService: NestService,
    @Inject private val eventRegistry: EventRegistry,
) : ImportExportService {
    override fun peekImportEggIdShells() = exchangeFactory.createPasswordExchange().receive().entries.associate {
        it.key.slot to it.value.map { bytePair -> bytePair.first }
    }

    override fun importEggs() {
        val currentNest = nestService.currentNest()
        val eggsByNest = exchangeFactory.createPasswordExchange().receive()
        eggsByNest.keys.forEach { nest ->
            val deployedNest = nestService.atNestSlot(nest.slot)
            if (deployedNest.isEmpty) {
                nestService.place(nest.viewNestId(), nest.slot)
            }
            nestService.moveToNestAt(nest.slot)
            passwordService.putEggs(eggsByNest[nest]!!.stream())
        }
        nestService.moveToNestAt(currentNest.slot)
        eventRegistry.register(EggsImported(eggsByNest.values.sumOf { it.size }))
        eventRegistry.processEvents()
    }

    override fun exportEggs() {
        val currentNest = nestService.currentNest()
        val eggsByNest = mutableMapOf<Nest, List<ShellPair>>()
        nestService.all(includeDefault = true).filter { it.isPresent }.map { it.get() }.forEach { nest ->
            nestService.moveToNestAt(nest.slot)
            eggsByNest[nest] = passwordService.findAllEggIds()
                .map { eggId -> ShellPair(eggId, passwordService.viewPassword(eggId).get()) }.toList()
        }
        exchangeFactory.createPasswordExchange().send(eggsByNest)
        nestService.moveToNestAt(currentNest.slot)
        eventRegistry.register(EggsExported(eggsByNest.values.sumOf { it.size }))
        eventRegistry.processEvents()
    }
}
