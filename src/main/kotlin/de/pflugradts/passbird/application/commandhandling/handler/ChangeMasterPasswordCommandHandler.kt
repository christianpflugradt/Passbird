package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ChangeMasterPasswordCommand
import de.pflugradts.passbird.application.failure.CommandFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.security.KeyStoreAuthenticationService
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.EVENT_HANDLED
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import jakarta.inject.Inject

class ChangeMasterPasswordCommandHandler @Inject constructor(
    private val keyStoreAdapterPort: KeyStoreAdapterPort,
    private val keyStoreAuthenticationService: KeyStoreAuthenticationService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleChangeMasterPasswordCommand(@Suppress("UNUSED_PARAMETER") changeMasterPasswordCommand: ChangeMasterPasswordCommand) {
        userInterfaceAdapterPort.sendLineBreak()
        userInterfaceAdapterPort.send(outputOf(shellOf(KEYSTORE_PREAMBLE)))
        userInterfaceAdapterPort.sendLineBreak()
        val key = keyStoreAuthenticationService.authenticate(maxAttempts = 1, prompt = "Enter current key: ").getOrNull()
        if (key == null) {
            abort("Current key is incorrect - Operation aborted.")
            return
        }
        try {
            val newPassword = receiveNewPassword()
            if (newPassword == null) {
                return
            }
            val storeResult = tryCatching {
                keyStoreAdapterPort.storeExistingKey(key, newPassword, keyStoreAuthenticationService.keyStorePath())
            }
            if (storeResult.failure) {
                CommandExecutionTracker.markFailure()
                reportFailure(CommandFailure(storeResult.exceptionOrNull()!!))
                abort("Operation aborted.")
                return
            }
        } finally {
            key.scramble()
        }
        userInterfaceAdapterPort.send(outputOf(shellOf("Keystore successfully updated."), EVENT_HANDLED))
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun receiveNewPassword(): PlainShell? {
        val input = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter new key: ")))
        if (input.isEmpty) {
            input.invalidate()
            abort("Empty input - Operation aborted.")
            return null
        }
        val repeatedInput = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter new key again: ")))
        if (repeatedInput.isEmpty) {
            input.invalidate()
            repeatedInput.invalidate()
            abort("Empty input - Operation aborted.")
            return null
        }
        if (input != repeatedInput) {
            input.invalidate()
            repeatedInput.invalidate()
            abort("Your inputs do not match - Operation aborted.")
            return null
        }
        val password = input.toPlainShell()
        repeatedInput.invalidate()
        userInterfaceAdapterPort.sendLineBreak()
        return password
    }

    private fun abort(message: String) {
        CommandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf(message), OPERATION_ABORTED))
        userInterfaceAdapterPort.sendLineBreak()
    }

    private companion object {
        const val KEYSTORE_PREAMBLE =
            "Your Passbird Keystore will be secured by a master password. This master password gives access to all " +
                "passwords stored in Passbird. If you lose this password, you will not be able to access any passwords " +
                "stored in Passbird. Choose your master password wisely. You have to input your master password twice. " +
                "Your input will be hidden unless secure input is disabled in your configuration."
    }
}
