package de.pflugradts.passbird.application.process.password

import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.domain.service.password.PasswordService

class TrashCleanup constructor(
    private val passwordService: PasswordService,
) : Initializer {
    override fun run() {
        passwordService.cleanupTrash()
    }
}
