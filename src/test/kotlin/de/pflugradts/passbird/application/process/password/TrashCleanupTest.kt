package de.pflugradts.passbird.application.process.password

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class TrashCleanupTest {

    private val passwordService = mockk<PasswordService>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)

    @Test
    fun `should trigger trash cleanup during initialization`() {
        fakePasswordService(instance = passwordService)

        TrashCleanup(passwordService, userInterfaceAdapterPort).run()

        verify(exactly = 1) { passwordService.cleanupTrash(any()) }
    }

    @Test
    fun `should announce trash cleanup before discarding eggs`() {
        every { passwordService.cleanupTrash(any()) } answers {
            firstArg<() -> Unit>().invoke()
            de.pflugradts.kotlinextensions.TryResult.success(1)
        }

        TrashCleanup(passwordService, userInterfaceAdapterPort).run()

        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Discarding Eggs from trash."))) }
    }
}
