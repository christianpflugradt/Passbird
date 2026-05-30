package de.pflugradts.passbird.application.commandhandling
class CommandExecutionTracker constructor() {
    private val executionOutcomes = ArrayDeque<CommandExecutionOutcome?>()
    private var lastCompletedOutcome: CommandExecutionOutcome? = null
    fun begin() {
        executionOutcomes.addLast(null)
    }
    fun finish(defaultOutcome: CommandExecutionOutcome): CommandExecutionOutcome {
        val outcome = if (executionOutcomes.isEmpty()) {
            defaultOutcome
        } else {
            executionOutcomes.removeLast() ?: defaultOutcome
        }
        lastCompletedOutcome = outcome
        return outcome
    }
    fun lastCompletedOutcome() = lastCompletedOutcome ?: CommandExecutionOutcome.FAILURE
    fun mark(outcome: CommandExecutionOutcome) {
        if (executionOutcomes.isNotEmpty()) {
            executionOutcomes.removeLast()
            executionOutcomes.addLast(outcome)
        }
    }
    fun markAborted() = mark(CommandExecutionOutcome.ABORTED)
    fun markFailure() = mark(CommandExecutionOutcome.FAILURE)
}
