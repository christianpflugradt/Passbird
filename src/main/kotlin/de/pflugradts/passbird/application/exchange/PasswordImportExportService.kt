package de.pflugradts.passbird.application.exchange

import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_SYSTEM_PROPERTY
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
    override fun peekImportEggIdShells() = exchangeFactory.createPasswordExchange(PASSBIRD_HOME_URI).receive().entries.associate {
        it.key to it.value.map { bytePair -> bytePair.first }
    }

    override fun importEggs() {
        val currentNest = nestService.currentNest()
        val eggsByNest = exchangeFactory.createPasswordExchange(PASSBIRD_HOME_URI).receive()
        eggsByNest.keys.forEach { nestSlot ->
            val deployedNest = nestService.atNestSlot(nestSlot)
            if (deployedNest.isEmpty) {
                nestService.place(shellOf("Nest-${nestSlot.index()}"), nestSlot)
            }
            nestService.moveToNestAt(nestSlot)
            passwordService.putEggs(eggsByNest[nestSlot]!!.stream())
        }
        nestService.moveToNestAt(currentNest.nestSlot)
    }

    override fun exportEggs() {
        val currentNest = nestService.currentNest()
        val eggsByNest = mutableMapOf<NestSlot, List<ShellPair>>()
        nestService.all(includeDefault = true).filter { it.isPresent }.map { it.get() }.forEach { nest ->
            nestService.moveToNestAt(nest.nestSlot)
            eggsByNest[nest.nestSlot] = passwordService.findAllEggIds()
                .map { eggId -> ShellPair(eggId, passwordService.viewPassword(eggId).get()) }.toList()
        }
        exchangeFactory.createPasswordExchange(PASSBIRD_HOME_URI).send(eggsByNest)
        nestService.moveToNestAt(currentNest.nestSlot)
    }

    companion object {
        private val PASSBIRD_HOME_URI = System.getProperty(CONFIGURATION_SYSTEM_PROPERTY)
    }
}
