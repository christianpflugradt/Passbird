package de.pflugradts.passbird.application.commandhandling.egg

import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.egg.ViewTrashCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.RestoreEggResult
import de.pflugradts.passbird.domain.service.password.TrashEggView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class ViewTrashCommandTest {

    private val configuration = mockk<Configuration>()
    private val nestService = createNestServiceForTesting()
    private val passwordService = mockk<PasswordService>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val commandExecutionTracker = CommandExecutionTracker()
    private val viewTrashCommandHandler = ViewTrashCommandHandler(
        configuration = configuration,
        nestService = nestService,
        passwordService = passwordService,
        userInterfaceAdapterPort = userInterfaceAdapterPort,
        commandExecutionTracker = commandExecutionTracker,
    )
    private val inputHandler = createInputHandlerFor(viewTrashCommandHandler, commandExecutionTracker)

    @Test
    fun `should restore selected trashed egg`() {
        // given
        nestService.place(shellOf("work"), S2)
        val alpha = TrashEggView(shellOf("alpha"), DEFAULT, 11)
        val beta = TrashEggView(shellOf("beta"), S2, 3)
        every { passwordService.viewTrash() } returnsMany listOf(listOf(alpha, beta), emptyList())
        every { passwordService.restoreEgg(shellOf("beta")) } returns success(RestoreEggResult.RESTORED)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withReceiveConfirmation = true,
        )
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shellOf("d")))

        // then
        verify(exactly = 1) {
            userInterfaceAdapterPort.receive(
                eq(
                    outputOf(
                        shellOf(
                            """
                            Trash
                            [0]	Default/alpha	11
                            [1]	work/beta	3
                            Enter index to restore Egg or just press enter to abort: 
                            """.trimIndent(),
                        ),
                    ),
                ),
            )
        }
        verify(exactly = 1) { passwordService.restoreEgg(any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Trash is empty")))) }
    }

    @Test
    fun `should exit trash view on empty input`() {
        // given
        every { passwordService.viewTrash() } returns listOf(TrashEggView(shellOf("alpha"), DEFAULT, 11))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(Input.emptyInput()))
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("d")))

        // then
        verify(exactly = 1) { passwordService.viewTrash() }
        verify(exactly = 0) { passwordService.restoreEgg(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
    }

    @Test
    fun `should abort restore on invalid trash index`() {
        // given
        every { passwordService.viewTrash() } returnsMany listOf(
            listOf(TrashEggView(shellOf("alpha"), DEFAULT, 11)),
            listOf(TrashEggView(shellOf("alpha"), DEFAULT, 11)),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("9")), Input.emptyInput()),
        )
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("d")))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        verify(exactly = 0) { passwordService.restoreEgg(any()) }
    }

    @Test
    fun `should abort restore when confirmation is rejected`() {
        // given
        every { passwordService.viewTrash() } returnsMany listOf(
            listOf(TrashEggView(shellOf("alpha"), DEFAULT, 11)),
            listOf(TrashEggView(shellOf("alpha"), DEFAULT, 11)),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("0")), Input.emptyInput()),
            withReceiveConfirmation = false,
        )
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shellOf("d")))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.receiveConfirmation(any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        verify(exactly = 0) { passwordService.restoreEgg(any()) }
    }

    @Test
    fun `should show fallback message when restoring to default nest`() {
        // given
        every { passwordService.viewTrash() } returnsMany listOf(
            listOf(TrashEggView(shellOf("alpha"), S2, 11)),
            emptyList(),
        )
        every { passwordService.restoreEgg(any()) } returns success(RestoreEggResult.RESTORED_TO_DEFAULT)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(shellOf("0"))))
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("d")))

        // then
        verify(exactly = 1) {
            userInterfaceAdapterPort.send(
                eq(outputOf(shellOf("Original Nest no longer exists. Egg will be restored to Default Nest."))),
            )
        }
        verify(exactly = 1) { passwordService.restoreEgg(any()) }
    }

    @Test
    fun `should show restore conflict message and keep trash open`() {
        // given
        every { passwordService.viewTrash() } returnsMany listOf(
            listOf(TrashEggView(shellOf("alpha"), DEFAULT, 11)),
            listOf(TrashEggView(shellOf("alpha"), DEFAULT, 11)),
        )
        every { passwordService.restoreEgg(any()) } returns success(RestoreEggResult.TARGET_CONFLICT)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("0")), Input.emptyInput()),
        )
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("d")))

        // then
        verify(exactly = 1) {
            userInterfaceAdapterPort.send(
                eq(outputOf(shellOf("Egg with same EggId already exists in target Nest - Operation aborted."))),
            )
        }
        verify(exactly = 1) { passwordService.restoreEgg(any()) }
    }

    @Test
    fun `should show empty trash`() {
        // given
        every { passwordService.viewTrash() } returns emptyList()
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("d")))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Trash is empty")))) }
        verify(exactly = 0) { passwordService.restoreEgg(any()) }
    }
}
