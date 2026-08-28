package de.pflugradts.passbird.application.process.password

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class TrashCleanup constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : Initializer {
    override fun run() {
        passwordService.cleanupTrash {
            userInterfaceAdapterPort.send(outputOf(shellOf("Discarding Eggs from trash.")))
        }
    }
}
