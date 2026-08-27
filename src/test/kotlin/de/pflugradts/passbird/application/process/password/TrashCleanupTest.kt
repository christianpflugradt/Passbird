package de.pflugradts.passbird.application.process.password

import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class TrashCleanupTest {

    private val passwordService = mockk<PasswordService>()

    @Test
    fun `should trigger trash cleanup during initialization`() {
        fakePasswordService(instance = passwordService)

        TrashCleanup(passwordService).run()

        verify(exactly = 1) { passwordService.cleanupTrash() }
    }
}
