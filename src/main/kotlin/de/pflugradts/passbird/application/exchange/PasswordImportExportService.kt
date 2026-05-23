package de.pflugradts.passbird.application.exchange

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class PasswordImportExportService @Inject constructor(
    private val exchangeFactory: ExchangeFactory,
    private val passwordService: PasswordService,
    private val nestService: NestService,
    private val eventRegistry: EventRegistry,
) : ImportExportService {
    override fun peekImportEggIdShells(): TryResult<ShellMap> = exchangeFactory.createPasswordExchange().receive().map { eggsByNest ->
        eggsByNest.entries.associate { it.key.slot to it.value.map { passwordInfo -> passwordInfo.first.first } }
    }

    override fun importEggs() {
        exchangeFactory.createPasswordExchange().receive().onSuccess { eggsByNest ->
            val currentNest = nestService.currentNest()
            try {
                eggsByNest.forEach { (nest, passwordInfos) ->
                    val deployedNest = nestService.atNestSlot(nest.slot)
                    if (deployedNest.isEmpty) {
                        nestService.place(nest.viewNestId(), nest.slot)
                    }
                    nestService.moveToNestAt(nest.slot)
                    passwordInfos.forEach { passwordInfo ->
                        passwordService.putEgg(passwordInfo.first.first, passwordInfo.first.second)
                        passwordInfo.second.forEachIndexed { index, shellPair ->
                            if (shellPair.first.isNotEmpty && shellPair.second.isNotEmpty) {
                                passwordService.putProtein(
                                    eggIdShell = passwordInfo.first.first,
                                    slot = slotAt(index),
                                    typeShell = shellPair.first,
                                    structureShell = shellPair.second,
                                )
                            }
                        }
                    }
                }
            } finally {
                nestService.moveToNestAt(currentNest.slot)
            }
            eventRegistry.register(EggsImported(eggsByNest.values.sumOf { it.size }))
            eventRegistry.processEvents()
        }
    }

    override fun exportEggs() {
        val currentNest = nestService.currentNest()
        val eggsByNest = mutableMapOf<Nest, List<PasswordInfo>>()
        try {
            nestService.all(includeDefault = true)
                .filter { it.isPresent }
                .map { it.get() }
                .forEach { nest ->
                    nestService.moveToNestAt(nest.slot)
                    eggsByNest[nest] = passwordService.findAllEggIds()
                        .map { eggId ->
                            PasswordInfo(
                                first = ShellPair(eggId, passwordService.viewPassword(eggId).get()),
                                second = passwordService.viewProteinTypes(eggId).toShellList()
                                    .zip(passwordService.viewProteinStructures(eggId).toShellList()),
                            )
                        }.toList()
                }
        } finally {
            nestService.moveToNestAt(currentNest.slot)
        }
        exchangeFactory.createPasswordExchange().send(eggsByNest).onSuccess {
            eventRegistry.register(EggsExported(eggsByNest.values.sumOf { it.size }))
            eventRegistry.processEvents()
        }
    }
}

fun Option<List<Option<Shell>>>.toShellList() = map { list -> list.map { it.orElse(emptyShell()) } }.orElse(List(10) { emptyShell() })
