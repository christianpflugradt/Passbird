package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService

class PasswordImportExportService @Inject constructor(
    @Inject private val exchangeFactory: ExchangeFactory,
    @Inject private val passwordService: PasswordService,
    @Inject private val nestService: NestService,
    @Inject private val eventRegistry: EventRegistry,
) : ImportExportService {
    override fun peekImportEggIdShells() = exchangeFactory.createPasswordExchange().receive().entries.associate {
        it.key to it.value.map { bytePair -> bytePair.first }
    }

    override fun importEggs() {
        val currentNest = nestService.currentNest()
        val eggsByNest = exchangeFactory.createPasswordExchange().receive()
        eggsByNest.keys.forEach { nestSlot ->
            val deployedNest = nestService.atNestSlot(nestSlot)
            if (deployedNest.isEmpty) {
                nestService.place(shellOf("Nest-${nestSlot.index()}"), nestSlot)
            }
            nestService.moveToNestAt(nestSlot)
            passwordService.putEggs(eggsByNest[nestSlot]!!.stream())
        }
        nestService.moveToNestAt(currentNest.nestSlot)
        eventRegistry.register(EggsImported(eggsByNest.values.sumOf { it.size }))
        eventRegistry.processEvents()
    }

    override fun exportEggs() {
        val currentNest = nestService.currentNest()
        val eggsByNest = mutableMapOf<NestSlot, List<ShellPair>>()
        nestService.all(includeDefault = true).filter { it.isPresent }.map { it.get() }.forEach { nest ->
            nestService.moveToNestAt(nest.nestSlot)
            eggsByNest[nest.nestSlot] = passwordService.findAllEggIds()
                .map { eggId -> ShellPair(eggId, passwordService.viewPassword(eggId).get()) }.toList()
        }
        exchangeFactory.createPasswordExchange().send(eggsByNest)
        nestService.moveToNestAt(currentNest.nestSlot)
        eventRegistry.register(EggsExported(eggsByNest.values.sumOf { it.size }))
        eventRegistry.processEvents()
    }
}
