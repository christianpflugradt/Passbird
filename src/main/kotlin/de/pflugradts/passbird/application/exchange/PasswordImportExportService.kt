package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.nest.Slot
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
        val currentNest = nestService.getCurrentNest()
        val eggsByNest = exchangeFactory.createPasswordExchange(uri).receive()
        eggsByNest.keys.forEach { slot ->
            val deployedNest = nestService.atSlot(slot)
            if (deployedNest.isEmpty) {
                nestService.deploy(shellOf("Namespace-${slot.index()}"), slot)
            }
            nestService.moveToNestAt(slot)
            passwordService.putEggs(eggsByNest[slot]!!.stream())
        }
        nestService.moveToNestAt(currentNest.slot)
    }

    override fun exportEggs(uri: String) {
        val currentNest = nestService.getCurrentNest()
        val eggsByNest = mutableMapOf<Slot, List<ShellPair>>()
        nestService.all(includeDefault = true).filter { it.isPresent }.map { it.get() }.forEach { nest ->
            nestService.moveToNestAt(nest.slot)
            eggsByNest[nest.slot] = passwordService.findAllEggIds()
                .map { eggId -> ShellPair(Pair(eggId, passwordService.viewPassword(eggId).get())) }.toList()
        }
        exchangeFactory.createPasswordExchange(uri).send(eggsByNest)
        nestService.moveToNestAt(currentNest.slot)
    }
}
