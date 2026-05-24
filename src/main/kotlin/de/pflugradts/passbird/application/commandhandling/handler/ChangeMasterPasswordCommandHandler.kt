package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ChangeMasterPasswordCommand
import de.pflugradts.passbird.application.failure.CommandFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.security.KeyStoreAuthenticationService
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import jakarta.inject.Inject

class ChangeMasterPasswordCommandHandler @Inject constructor(
    private val keyStoreAdapterPort: KeyStoreAdapterPort,
    private val keyStoreAuthenticationService: KeyStoreAuthenticationService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleChangeMasterPasswordCommand(@Suppress("UNUSED_PARAMETER") changeMasterPasswordCommand: ChangeMasterPasswordCommand) {
        val key = keyStoreAuthenticationService.authenticate(maxAttempts = 3).getOrNull()
        if (key == null) {
            abort()
            return
        }
        try {
            val newPassword = receiveNewPassword()
            if (newPassword == null) {
                abort()
                return
            }
            keyStoreAdapterPort.storeExistingKey(key, newPassword, keyStoreAuthenticationService.keyStorePath())
        } catch (ex: Exception) {
            reportFailure(CommandFailure(ex))
            abort()
            return
        } finally {
            key.scramble()
        }
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun receiveNewPassword(): PlainShell? {
        while (true) {
            val input = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("first input: ")))
            if (input.isEmpty) {
                input.invalidate()
                return null
            }
            val repeatedInput = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("second input: ")))
            if (repeatedInput.isEmpty) {
                input.invalidate()
                repeatedInput.invalidate()
                return null
            }
            if (input == repeatedInput) {
                val password = input.toPlainShell()
                repeatedInput.invalidate()
                userInterfaceAdapterPort.sendLineBreak()
                return password
            }
            input.invalidate()
            repeatedInput.invalidate()
            userInterfaceAdapterPort.send(outputOf(shellOf("Your inputs do not match, please repeat.")))
        }
    }

    private fun abort() {
        userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        userInterfaceAdapterPort.sendLineBreak()
    }
}
