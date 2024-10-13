package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.MutableOption.Companion.emptyOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.ShellComparator
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import java.util.stream.Stream

class ViewPasswordService @Inject constructor(
    cryptoProvider: CryptoProvider,
    private val eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun findAllEggIds(): Stream<Shell> = eggRepository.findAll().map { decrypted(it.viewEggId()) }.sorted(ShellComparator())
    fun viewPassword(eggIdShell: Shell): Option<Shell> = extractFromEgg(eggIdShell) { decrypted(it.viewPassword()) }
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

    private fun <T> extractFromEgg(eggIdShell: Shell, extraction: (egg: Egg) -> T): Option<T> = find(eggIdShell)
        .map { extraction(it) }.or {
            eventRegistry.register(EggNotFound(eggIdShell))
            eventRegistry.processEvents()
            emptyOption()
        }
}
