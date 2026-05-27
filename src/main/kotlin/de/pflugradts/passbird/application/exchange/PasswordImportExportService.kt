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
import de.pflugradts.passbird.domain.model.slot.Slot
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
    override fun peekImportEggIdShells(): TryResult<ShellMap> = receiveImportData().map(::toShellMap)

    override fun peekImportNests(): TryResult<List<ImportNestPreview>> = receiveImportData().map { eggsByNest ->
        eggsByNest.entries.map { (nest, passwordInfos) ->
            ImportNestPreview(
                nestId = nest.viewNestId(),
                slot = nest.slot,
                eggIds = passwordInfos.map { passwordInfo -> passwordInfo.first.first },
            )
        }
    }

    override fun importEggs() {
        receiveImportData().onSuccess { eggsByNest ->
            importEggs(eggsByNest)
        }
    }

    override fun importEggs(sourceSlot: Slot, targetSlot: Slot) {
        receiveImportData().onSuccess { eggsByNest ->
            eggsByNest.entries.firstOrNull { (nest, _) -> nest.slot == sourceSlot }?.let { (nest, passwordInfos) ->
                importEggs(listOf(Triple(nest, targetSlot, passwordInfos)))
            }
        }
    }

    override fun exportEggs() = exportEggs(allNestSlots())

    override fun exportEggs(slots: Set<Slot>) {
        if (slots.isEmpty()) {
            return
        }
        val currentNest = nestService.currentNest()
        val eggsByNest = mutableMapOf<Nest, List<PasswordInfo>>()
        try {
            nestService.all(includeDefault = true)
                .filter { it.isPresent }
                .map { it.get() }
                .filter { it.slot in slots }
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

    private fun receiveImportData() = exchangeFactory.createPasswordExchange().receive()

    private fun toShellMap(eggsByNest: Map<Nest, List<PasswordInfo>>) = eggsByNest.entries.associate { (nest, passwordInfos) ->
        nest.slot to passwordInfos.map { passwordInfo -> passwordInfo.first.first }
    }

    private fun importEggs(eggsByNest: Map<Nest, List<PasswordInfo>>) {
        importEggs(eggsByNest.entries.map { (nest, passwordInfos) -> Triple(nest, nest.slot, passwordInfos) })
    }

    private fun importEggs(imports: List<Triple<Nest, Slot, List<PasswordInfo>>>) {
        if (imports.isEmpty()) {
            return
        }
        val currentNest = nestService.currentNest()
        var importedEggCount = 0
        try {
            imports.forEach { (nest, targetSlot, passwordInfos) ->
                val deployedNest = nestService.atNestSlot(targetSlot)
                if (deployedNest.isEmpty) {
                    nestService.place(nest.viewNestId(), targetSlot)
                }
                nestService.moveToNestAt(targetSlot)
                passwordInfos.forEach { passwordInfo ->
                    if (passwordService.putEgg(passwordInfo.first.first, passwordInfo.first.second).failure) {
                        eventRegistry.clearEvents()
                        return
                    }
                    if (!importProteins(passwordInfo)) {
                        return
                    }
                    importedEggCount++
                }
            }
        } finally {
            nestService.moveToNestAt(currentNest.slot)
        }
        eventRegistry.register(EggsImported(importedEggCount))
        eventRegistry.processEvents()
    }

    private fun importProteins(passwordInfo: PasswordInfo): Boolean {
        passwordInfo.second.forEachIndexed { index, shellPair ->
            if (shellPair.first.isNotEmpty && shellPair.second.isNotEmpty) {
                if (passwordService.putProtein(
                        eggIdShell = passwordInfo.first.first,
                        slot = slotAt(index),
                        typeShell = shellPair.first,
                        structureShell = shellPair.second,
                    ).failure
                ) {
                    eventRegistry.clearEvents()
                    return false
                }
            }
        }
        return true
    }

    private fun allNestSlots() = nestService.all(includeDefault = true)
        .filter { it.isPresent }
        .map { it.get().slot }
        .toList()
        .toSet()
}

fun Option<List<Option<Shell>>>.toShellList() = map { list -> list.map { it.orElse(emptyShell()) } }.orElse(List(10) { emptyShell() })
