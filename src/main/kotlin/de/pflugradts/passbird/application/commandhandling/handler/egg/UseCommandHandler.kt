package de.pflugradts.passbird.application.commandhandling.handler.egg

import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.GlobalHotkeyAdapterPort
import de.pflugradts.passbird.application.RegisteredGlobalHotkey
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.UseCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.application.yolk.LiveYolkView
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.SPECIAL
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.YolkView
import java.util.Locale

class UseCommandHandler(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val globalHotkeyAdapterPort: GlobalHotkeyAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val registerInteraction: () -> Unit,
    private val liveYolkView: LiveYolkView,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<UseCommand>(UseCommand::class.java) {
    override fun handleCommand(command: UseCommand) {
        if (!passwordService.eggExists(command.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) return finish(command)
        val steps = flowSteps(command.argument)
        if (steps.isEmpty()) {
            abort("Guided flow could not be continued")
            return finish(command)
        }
        val registeredHotkey = registerHotkeyIfNeeded(steps)
        try {
            renderInstruction(steps, registeredHotkey != null)
            steps.forEachIndexed { index, step ->
                if (!execute(step, registeredHotkey, index < steps.lastIndex)) {
                    return
                }
                if (step !is Step.Yolk && index < steps.lastIndex) {
                    userInterfaceAdapterPort.sendLineBreak()
                }
            }
        } catch (_: Exception) {
            abort("Guided flow could not be completed")
        } finally {
            runCatching { registeredHotkey?.release() }
            finish(command)
        }
    }

    private fun flowSteps(eggId: Shell): List<Step> = buildList {
        passwordService.viewProteinStructure(eggId, slotAt(configuration.application.flow.loginProteinSlot))
            .takeIf { it.isPresent && it.get().isNotEmpty }
            ?.orNull()
            ?.copy()
            ?.let { add(Step.Copy("Login", it)) }
        passwordService.viewPassword(eggId)
            .orNull()
            ?.copy()
            ?.let { add(Step.Copy("Password", it)) }
        passwordService.viewYolk(eggId).orNull()?.let { add(Step.Yolk(it)) }
    }

    private fun renderInstruction(steps: List<Step>, hotkeyRegistered: Boolean) {
        if (steps.size <= 1) return
        if (!configuration.application.flow.globalHotkey.enabled) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Press Enter to continue.")))
            userInterfaceAdapterPort.sendLineBreak()
            return
        }
        if (!hotkeyRegistered) {
            val key = hotkeyKey()
            userInterfaceAdapterPort.send(outputOf(shellOf("Global hotkey Ctrl+Shift+$key could not be registered."), SPECIAL))
            userInterfaceAdapterPort.send(outputOf(shellOf("Press Enter to continue."), DEFAULT))
            userInterfaceAdapterPort.sendLineBreak()
            return
        }
        val key = hotkeyKey()
        userInterfaceAdapterPort.send(
            outputOf(shellOf("Ctrl+Shift+$key"), HIGHLIGHT),
            outputOf(shellOf(" or "), DEFAULT),
            outputOf(shellOf("Enter"), HIGHLIGHT),
            outputOf(shellOf(" to continue"), DEFAULT),
        )
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun registerHotkeyIfNeeded(steps: List<Step>): RegisteredGlobalHotkey? {
        if (steps.size <= 1 || !configuration.application.flow.globalHotkey.enabled) {
            return null
        }
        return globalHotkeyAdapterPort.register(hotkeyKeyChar())
    }

    private fun execute(step: Step, registeredHotkey: RegisteredGlobalHotkey?, hasMoreSteps: Boolean): Boolean = when (step) {
        is Step.Copy -> copy(step, registeredHotkey, hasMoreSteps)
        is Step.Yolk -> showYolk(step.yolkView, registeredHotkey)
    }

    private fun copy(step: Step.Copy, registeredHotkey: RegisteredGlobalHotkey?, hasMoreSteps: Boolean): Boolean {
        var copied = false
        step.secret.useScrambled {
            val clipboardResult = clipboardAdapterPort.post(outputOf(it))
            if (clipboardResult.failure) {
                abort("${step.label} could not be copied to clipboard")
                return false
            }
            copied = true
            userInterfaceAdapterPort.send(
                outputOf(shellOf(step.label), HIGHLIGHT),
                outputOf(shellOf(" copied to clipboard."), DEFAULT),
            )
        }
        if (!copied) return false
        if (!hasMoreSteps) {
            return true
        }
        return awaitNextAction(registeredHotkey, WAIT_FOREVER_MILLISECONDS)
    }

    private fun showYolk(yolkView: YolkView, registeredHotkey: RegisteredGlobalHotkey?): Boolean {
        userInterfaceAdapterPort.send(
            outputOf(shellOf("Yolk"), HIGHLIGHT),
            outputOf(shellOf(" copied to clipboard."), DEFAULT),
        )
        return runCatching {
            liveYolkView.show(yolkView) { awaitNextAction(registeredHotkey, it) }
        }.fold(
            onSuccess = { true },
            onFailure = {
                abort("Yolk could not be displayed")
                false
            },
        )
    }

    private fun awaitNextAction(registeredHotkey: RegisteredGlobalHotkey?, milliseconds: Long): Boolean {
        var remainingMilliseconds = milliseconds
        while (true) {
            val pollInterval = remainingMilliseconds.coerceAtMost(POLL_INTERVAL_MILLISECONDS)
            if (registeredHotkey?.awaitWithin(pollInterval) == true) {
                registerInteraction()
                return true
            }
            if (userInterfaceAdapterPort.receiveLineBreakWithin(pollInterval)) {
                registerInteraction()
                return true
            }
            if (remainingMilliseconds != WAIT_FOREVER_MILLISECONDS) {
                remainingMilliseconds -= pollInterval
            }
            if (remainingMilliseconds == 0L) {
                return false
            }
        }
    }

    private fun abort(reason: String) {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("$reason - Operation aborted."), OPERATION_ABORTED))
    }

    private fun finish(command: UseCommand) {
        command.invalidateInput()
    }

    private fun hotkeyKey() = configuration.application.flow.globalHotkey.key.uppercase(Locale.ROOT)
    private fun hotkeyKeyChar() = hotkeyKey().single()

    private sealed interface Step {
        data class Copy(val label: String, val secret: Shell) : Step
        data class Yolk(val yolkView: YolkView) : Step
    }

    companion object {
        private const val POLL_INTERVAL_MILLISECONDS = 50L
        private const val WAIT_FOREVER_MILLISECONDS = Long.MAX_VALUE
    }
}
