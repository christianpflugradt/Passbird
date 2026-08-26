package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.TryResult
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
    fun viewNestStats(): NestStats
    fun viewNestStats(slot: Slot): NestStats
    fun viewFavorites(): Slots<Shell>
    fun viewFavoriteEntry(slot: Slot): Option<Shell>
    fun viewMemory(): Slots<Shell>
    fun putFavorite(slot: Slot, eggIdShell: Shell): TryResult<Unit>
    fun viewMemoryEntry(slot: Slot): Option<Shell>
    fun viewPassword(eggIdShell: Shell): Option<Shell>
    fun viewProteinStructure(eggIdShell: Shell, slot: Slot): Option<Shell>
    fun viewProteinStructures(eggIdShell: Shell): Option<List<Option<Shell>>>
    fun viewProteinType(eggIdShell: Shell, slot: Slot): Option<Shell>
    fun viewProteinTypes(eggIdShell: Shell): Option<List<Option<Shell>>>
    fun viewYolk(eggIdShell: Shell): Option<YolkView>
    fun renameEgg(eggIdShell: Shell, newEggIdShell: Shell): TryResult<Unit>
    fun challengeEggId(shell: Shell)
    fun putEggs(eggs: Stream<ShellPair>): TryResult<Unit>
    fun putEgg(eggIdShell: Shell, passwordShell: Shell): TryResult<Unit>
    fun putProtein(eggIdShell: Shell, slot: Slot, typeShell: Shell, structureShell: Shell): TryResult<Unit>
    fun putProteins(eggIdShell: Shell, proteins: List<ProteinEntry>): TryResult<Unit>
    fun putYolk(eggIdShell: Shell, secretShell: Shell, algorithm: String, digits: Int, periodSeconds: Int): TryResult<Unit>
    fun discardFavorite(slot: Slot): TryResult<Unit>
    fun discardEgg(eggIdShell: Shell): TryResult<Unit>
    fun discardProtein(eggIdShell: Shell, slot: Slot): TryResult<Unit>
    fun discardYolk(eggIdShell: Shell): TryResult<Unit>
    fun moveEgg(eggIdShell: Shell, targetSlot: Slot): TryResult<Unit>
    fun findAllEggIds(): Stream<Shell>
    fun findAllEggIds(slot: Slot): Stream<Shell>
}

data class NestStats(
    val eggs: Int,
    val eggsWithYolks: Int,
    val eggsWithProteins: Int,
    val occupiedProteinSlots: Int,
    val assignedFavorites: Int,
)

data class YolkView(
    val secret: Shell,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
)
