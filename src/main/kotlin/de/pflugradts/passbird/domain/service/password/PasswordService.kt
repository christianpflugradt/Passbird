package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots
import java.util.stream.Stream

interface PasswordService {
    enum class EggNotExistsAction {
        DO_NOTHING,
        CREATE_ENTRY_NOT_EXISTS_EVENT,
    }

    fun eggExists(eggIdShell: Shell, slot: Slot): Boolean
    fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction): Boolean
    fun proteinExists(eggIdShell: Shell, slot: Slot): Boolean
    fun viewMemory(): Slots<Shell>
    fun viewPassword(eggIdShell: Shell): Option<Shell>
    fun viewProteinStructure(eggIdShell: Shell, slot: Slot): Option<Shell>
    fun viewProteinStructures(eggIdShell: Shell): Option<List<Option<Shell>>>
    fun viewProteinType(eggIdShell: Shell, slot: Slot): Option<Shell>
    fun viewProteinTypes(eggIdShell: Shell): Option<List<Option<Shell>>>
    fun renameEgg(eggIdShell: Shell, newEggIdShell: Shell)
    fun challengeEggId(shell: Shell)
    fun putEggs(eggs: Stream<ShellPair>)
    fun putEgg(eggIdShell: Shell, passwordShell: Shell)
    fun putProtein(eggIdShell: Shell, slot: Slot, typeShell: Shell, structureShell: Shell)
    fun discardEgg(eggIdShell: Shell)
    fun discardProtein(eggIdShell: Shell, slot: Slot)
    fun moveEgg(eggIdShell: Shell, targetSlot: Slot)
    fun findAllEggIds(): Stream<Shell>
}
