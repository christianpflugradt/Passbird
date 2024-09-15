package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import java.util.stream.Stream

class PasswordFacade @Inject constructor(
    private val putPasswordService: PutPasswordService,
    private val viewPasswordService: ViewPasswordService,
    private val discardPasswordService: DiscardPasswordService,
    private val renamePasswordService: RenamePasswordService,
    private val movePasswordService: MovePasswordService,
) : PasswordService {
    override fun eggExists(eggIdShell: Shell, slot: Slot) = viewPasswordService.eggExists(eggIdShell, slot)
    override fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction) =
        viewPasswordService.eggExists(eggIdShell, eggNotExistsAction)
    override fun proteinExists(eggIdShell: Shell, slot: Slot) = viewPasswordService.proteinExists(eggIdShell, slot)
    override fun viewPassword(eggIdShell: Shell) = viewPasswordService.viewPassword(eggIdShell)
    override fun viewProteinStructure(eggIdShell: Shell, slot: Slot) = viewPasswordService.viewProteinStructure(eggIdShell, slot)
    override fun viewProteinStructures(eggIdShell: Shell) = viewPasswordService.viewProteinStructures(eggIdShell)
    override fun viewProteinType(eggIdShell: Shell, slot: Slot) = viewPasswordService.viewProteinType(eggIdShell, slot)
    override fun viewProteinTypes(eggIdShell: Shell) = viewPasswordService.viewProteinTypes(eggIdShell)
    override fun renameEgg(eggIdShell: Shell, newEggIdShell: Shell) = renamePasswordService.renameEgg(eggIdShell, newEggIdShell)
    override fun findAllEggIds() = viewPasswordService.findAllEggIds()
    override fun challengeEggId(shell: Shell) = putPasswordService.challengeEggId(shell)
    override fun putEggs(eggs: Stream<ShellPair>) = putPasswordService.putEggs(eggs)
    override fun putEgg(eggIdShell: Shell, passwordShell: Shell) = putPasswordService.putEgg(eggIdShell, passwordShell)
    override fun putProtein(eggIdShell: Shell, slot: Slot, typeShell: Shell, structureShell: Shell) =
        putPasswordService.putProtein(eggIdShell, slot, typeShell, structureShell)
    override fun discardEgg(eggIdShell: Shell) = discardPasswordService.discardEgg(eggIdShell)
    override fun moveEgg(eggIdShell: Shell, targetSlot: Slot) = movePasswordService.movePassword(eggIdShell, targetSlot)
}
