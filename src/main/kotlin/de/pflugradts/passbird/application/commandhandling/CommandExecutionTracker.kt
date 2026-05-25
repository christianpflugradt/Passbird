package de.pflugradts.passbird.application.commandhandling

object CommandExecutionTracker {
    private val executionOutcomes = ThreadLocal.withInitial { ArrayDeque<CommandExecutionOutcome?>() }
    private val lastCompletedOutcome = ThreadLocal.withInitial<CommandExecutionOutcome?> { null }

    fun begin() {
        executionOutcomes.get().addLast(null)
    }

    fun finish(defaultOutcome: CommandExecutionOutcome): CommandExecutionOutcome {
        val outcomes = executionOutcomes.get()
        val outcome = if (outcomes.isEmpty()) {
            defaultOutcome
        } else {
            outcomes.removeLast() ?: defaultOutcome
        }
        lastCompletedOutcome.set(outcome)
        return outcome
    }

    fun lastCompletedOutcome() = lastCompletedOutcome.get() ?: CommandExecutionOutcome.FAILURE

    fun mark(outcome: CommandExecutionOutcome) {
        executionOutcomes.get().also {
            if (it.isNotEmpty()) {
                it.removeLast()
                it.addLast(outcome)
            }
        }
    }

    fun markAborted() = mark(CommandExecutionOutcome.ABORTED)
    fun markFailure() = mark(CommandExecutionOutcome.FAILURE)
}
