package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import java.util.Optional
import java.util.stream.Stream

interface PasswordService {
    enum class EggNotExistsAction {
        DO_NOTHING,
        CREATE_ENTRY_NOT_EXISTS_EVENT,
    }

    fun eggExists(eggIdShell: Shell, nestSlot: Slot): Boolean
    fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction): Boolean
    fun viewPassword(eggIdShell: Shell): Optional<Shell>
    fun renameEgg(eggIdShell: Shell, newEggIdShell: Shell)
    fun challengeEggId(shell: Shell)
    fun putEggs(eggs: Stream<ShellPair>)
    fun putEgg(eggIdShell: Shell, passwordShell: Shell)
    fun discardEgg(eggIdShell: Shell)
    fun moveEgg(eggIdShell: Shell, targetNestSlot: Slot)
    fun findAllEggIds(): Stream<Shell>
}
