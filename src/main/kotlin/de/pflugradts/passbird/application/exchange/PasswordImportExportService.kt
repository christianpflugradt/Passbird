package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService

class PasswordImportExportService @Inject constructor(
    @Inject private val exchangeFactory: ExchangeFactory,
    @Inject private val passwordService: PasswordService,
    @Inject private val nestService: NestService,
) : ImportExportService {
    override fun peekImportEggIdShells(uri: String) = exchangeFactory.createPasswordExchange(uri).receive().entries.associate {
        it.key to it.value.map { bytePair -> bytePair.value.first }
    }

    override fun importEggs(uri: String) {
        val currentNest = nestService.currentNest()
        val eggsByNest = exchangeFactory.createPasswordExchange(uri).receive()
        eggsByNest.keys.forEach { nestSlot ->
            val deployedNest = nestService.atNestSlot(nestSlot)
            if (deployedNest.isEmpty) {
                nestService.place(shellOf("Namespace-${nestSlot.index()}"), nestSlot)
            }
            nestService.moveToNestAt(nestSlot)
            passwordService.putEggs(eggsByNest[nestSlot]!!.stream())
        }
        nestService.moveToNestAt(currentNest.nestSlot)
    }

    override fun exportEggs(uri: String) {
        val currentNest = nestService.currentNest()
        val eggsByNest = mutableMapOf<NestSlot, List<ShellPair>>()
        nestService.all(includeDefault = true).filter { it.isPresent }.map { it.get() }.forEach { nest ->
            nestService.moveToNestAt(nest.nestSlot)
            eggsByNest[nest.nestSlot] = passwordService.findAllEggIds()
                .map { eggId -> ShellPair(Pair(eggId, passwordService.viewPassword(eggId).get())) }.toList()
        }
        exchangeFactory.createPasswordExchange(uri).send(eggsByNest)
        nestService.moveToNestAt(currentNest.nestSlot)
    }
}
