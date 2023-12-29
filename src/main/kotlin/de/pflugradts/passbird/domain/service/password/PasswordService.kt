package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import java.util.stream.Stream

interface PasswordService {
    enum class EggNotExistsAction {
        DO_NOTHING,
        CREATE_ENTRY_NOT_EXISTS_EVENT,
    }

    fun eggExists(eggIdShell: Shell, nestSlot: NestSlot): Boolean
    fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction): Boolean
    fun viewPassword(eggIdShell: Shell): Option<Shell>
    fun renameEgg(eggIdShell: Shell, newEggIdShell: Shell)
    fun challengeEggId(shell: Shell)
    fun putEggs(eggs: Stream<ShellPair>)
    fun putEgg(eggIdShell: Shell, passwordShell: Shell)
    fun discardEgg(eggIdShell: Shell)
    fun moveEgg(eggIdShell: Shell, targetNestSlot: NestSlot)
    fun findAllEggIds(): Stream<Shell>
}
