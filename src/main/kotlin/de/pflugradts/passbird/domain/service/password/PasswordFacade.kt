package de.pflugradts.passbird.domain.service.password
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import java.util.stream.Stream
class PasswordFacade constructor(
    private val favoritePasswordService: FavoritePasswordService,
    private val putPasswordService: PutPasswordService,
    private val viewPasswordService: ViewPasswordService,
    private val discardPasswordService: DiscardPasswordService,
    private val renamePasswordService: RenamePasswordService,
    private val movePasswordService: MovePasswordService,
    private val currentEpochDaySupplier: () -> Int,
) : PasswordService {
    override fun eggExists(eggIdShell: Shell, slot: Slot) = viewPasswordService.eggExists(eggIdShell, slot)
    override fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction) =
        viewPasswordService.eggExists(eggIdShell, eggNotExistsAction)
    override fun proteinExists(eggIdShell: Shell, slot: Slot) = viewPasswordService.proteinExists(eggIdShell, slot)
    override fun viewNestStats() = viewPasswordService.viewNestStats()
    override fun viewNestStats(slot: Slot) = viewPasswordService.viewNestStats(slot)
    override fun viewFavorites() = favoritePasswordService.viewFavorites()
    override fun viewFavoriteEntry(slot: Slot) = favoritePasswordService.viewFavoriteEntry(slot)
    override fun viewMemory() = viewPasswordService.viewMemory()
    override fun putFavorite(slot: Slot, eggIdShell: Shell) = favoritePasswordService.putFavorite(slot, eggIdShell)
    override fun viewMemoryEntry(slot: Slot) = viewPasswordService.viewMemoryEntry(slot)
    override fun viewPassword(eggIdShell: Shell) = viewPasswordService.viewPassword(eggIdShell)
    override fun viewTrash() = viewPasswordService.viewTrash(currentEpochDaySupplier())
    override fun viewProteinStructure(eggIdShell: Shell, slot: Slot) = viewPasswordService.viewProteinStructure(eggIdShell, slot)
    override fun viewProteinStructures(eggIdShell: Shell) = viewPasswordService.viewProteinStructures(eggIdShell)
    override fun viewProteinType(eggIdShell: Shell, slot: Slot) = viewPasswordService.viewProteinType(eggIdShell, slot)
    override fun viewProteinTypes(eggIdShell: Shell) = viewPasswordService.viewProteinTypes(eggIdShell)
    override fun viewYolk(eggIdShell: Shell) = viewPasswordService.viewYolk(eggIdShell)
    override fun renameEgg(eggIdShell: Shell, newEggIdShell: Shell): TryResult<Unit> =
        renamePasswordService.renameEgg(eggIdShell, newEggIdShell)
    override fun findAllEggIds() = viewPasswordService.findAllEggIds()
    override fun findAllEggIds(slot: Slot) = viewPasswordService.findAllEggIds(slot)
    override fun challengeEggId(shell: Shell) = putPasswordService.challengeEggId(shell)
    override fun putEggs(eggs: Stream<ShellPair>): TryResult<Unit> = putPasswordService.putEggs(eggs)
    override fun putEgg(eggIdShell: Shell, passwordShell: Shell): TryResult<Unit> = putPasswordService.putEgg(eggIdShell, passwordShell)
    override fun putProtein(eggIdShell: Shell, slot: Slot, typeShell: Shell, structureShell: Shell): TryResult<Unit> =
        putPasswordService.putProtein(eggIdShell, slot, typeShell, structureShell)
    override fun putProteins(eggIdShell: Shell, proteins: List<ProteinEntry>): TryResult<Unit> =
        putPasswordService.putProteins(eggIdShell, proteins)
    override fun putYolk(eggIdShell: Shell, secretShell: Shell, algorithm: String, digits: Int, periodSeconds: Int): TryResult<Unit> =
        putPasswordService.putYolk(eggIdShell, secretShell, algorithm, digits, periodSeconds)
    override fun discardFavorite(slot: Slot): TryResult<Unit> = favoritePasswordService.discardFavorite(slot)
    override fun discardEgg(eggIdShell: Shell): TryResult<Unit> = discardPasswordService.discardEgg(eggIdShell, currentEpochDaySupplier())
    override fun discardEggPermanently(eggIdShell: Shell): TryResult<Unit> = discardPasswordService.discardEggPermanently(eggIdShell)
    override fun cleanupTrash(onDiscarding: () -> Unit): TryResult<Int> = discardPasswordService.cleanupTrash(onDiscarding)
    override fun discardProtein(eggIdShell: Shell, slot: Slot): TryResult<Unit> = discardPasswordService.discardProtein(eggIdShell, slot)
    override fun discardYolk(eggIdShell: Shell): TryResult<Unit> = discardPasswordService.discardYolk(eggIdShell)
    override fun moveEgg(eggIdShell: Shell, targetSlot: Slot): TryResult<Unit> = movePasswordService.movePassword(eggIdShell, targetSlot)
    override fun restoreEgg(eggIdShell: Shell) = discardPasswordService.restoreEgg(eggIdShell)
}
