package de.pflugradts.passbird.domain.service.password
import de.pflugradts.kotlinextensions.MutableOption.Companion.emptyOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.ShellComparator
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.toSlots
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import java.util.stream.Stream
class ViewPasswordService constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    eventRegistry: EventRegistry,
    memoryUpdateControl: MemoryUpdateControl,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry, memoryUpdateControl) {
    fun findAllEggIds(): Stream<Shell> = eggRepository.findAll().map { decrypted(it.viewEggId()) }.sorted(ShellComparator())
    fun findAllEggIds(slot: Slot): Stream<Shell> = eggRepository.findAll(slot).map { decrypted(it.viewEggId()) }.sorted(ShellComparator())
    fun viewNestStats() = eggRepository.findAll().toList().toNestStats(eggRepository.favorites())
    fun viewNestStats(slot: Slot) = eggRepository.findAll(slot).toList().toNestStats(eggRepository.favorites(slot))
    fun viewPassword(eggIdShell: Shell): Option<Shell> = extractFromEgg(eggIdShell) { decrypted(it.viewPassword()) }
    fun viewTrash(currentEpochDay: Int) = eggRepository.findAllTrashed().toList()
        .sortedBy(Egg::deletionEpochDay)
        .map { egg ->
            val eggId = decrypted(egg.viewEggId())
            TrashEggView(eggId = eggId, nestSlot = egg.associatedNest(), deletionAgeDays = currentEpochDay - egg.deletionEpochDay())
        }
    fun proteinExists(eggIdShell: Shell, slot: Slot) = viewProteinStructure(eggIdShell, slot).map { it.isNotEmpty }.orElse(false)
    fun viewProteinStructure(eggIdShell: Shell, slot: Slot): Option<Shell> = extractFromEgg(eggIdShell) { egg ->
        egg.proteins[slot.index()].map { decrypted(it.viewStructure()) }.orElse(emptyShell())
    }
    fun viewProteinStructures(eggIdShell: Shell) = extractFromEgg(eggIdShell) { egg ->
        egg.proteins.map { protein -> protein.map { optionOf(decrypted(it.viewStructure())) }.orElse(emptyOption()) }
    }
    fun viewProteinType(eggIdShell: Shell, slot: Slot): Option<Shell> = extractFromEgg(eggIdShell) { egg ->
        egg.proteins[slot.index()].map { decrypted(it.viewType()) }.orElse(emptyShell())
    }
    fun viewProteinTypes(eggIdShell: Shell) = extractFromEgg(eggIdShell) { egg ->
        egg.proteins.map { protein -> protein.map { optionOf(decrypted(it.viewType())) }.orElse(emptyOption()) }
    }
    fun viewYolk(eggIdShell: Shell): Option<YolkView> = find(eggIdShell).let { eggOption ->
        if (eggOption.isPresent) {
            eggOption.get().viewYolk().map {
                YolkView(
                    secret = decrypted(it.viewSecret()),
                    algorithm = it.algorithm,
                    digits = it.digits,
                    periodSeconds = it.periodSeconds,
                )
            }
        } else {
            eventRegistry.register(EggNotFound(eggIdShell))
            eventRegistry.processEvents()
            emptyOption()
        }
    }
    fun viewMemory() = eggRepository.memory().map { it.map { encryptedShell -> decrypted(encryptedShell) } }.toSlots()
    fun viewMemoryEntry(slot: Slot) = eggRepository.memory()[slot].map { decrypted(it) }
    private fun <T> extractFromEgg(eggIdShell: Shell, extraction: (egg: Egg) -> T): Option<T> = find(eggIdShell)
        .map { extraction(it) }.or {
            eventRegistry.register(EggNotFound(eggIdShell))
            eventRegistry.processEvents()
            emptyOption()
        }
}

private fun List<Egg>.toNestStats(favorites: EggIdFavorites) = NestStats(
    eggs = size,
    eggsWithYolks = count(Egg::hasYolk),
    eggsWithProteins = count { egg -> egg.proteins.any { it.isPresent } },
    occupiedProteinSlots = sumOf { egg -> egg.proteins.count { it.isPresent } },
    assignedFavorites = favorites.count { it.isPresent },
)
