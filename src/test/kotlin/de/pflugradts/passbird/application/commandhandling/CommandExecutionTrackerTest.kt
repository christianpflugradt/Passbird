package de.pflugradts.passbird.application.commandhandling

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class CommandExecutionTrackerTest {

    @Test
    fun `should keep command outcomes isolated per tracker instance`() {
        val firstTracker = CommandExecutionTracker()
        val secondTracker = CommandExecutionTracker()

        firstTracker.begin()
        firstTracker.finish(CommandExecutionOutcome.SUCCESS)

        expectThat(firstTracker.lastCompletedOutcome()) isEqualTo CommandExecutionOutcome.SUCCESS
        expectThat(secondTracker.lastCompletedOutcome()) isEqualTo CommandExecutionOutcome.FAILURE
    }

    @Test
    fun `should preserve outer command outcome across nested command execution`() {
        val commandExecutionTracker = CommandExecutionTracker()

        commandExecutionTracker.begin()
        commandExecutionTracker.begin()
        commandExecutionTracker.markFailure()
        val nestedOutcome = commandExecutionTracker.finish(CommandExecutionOutcome.SUCCESS)
        commandExecutionTracker.mark(nestedOutcome)
        val outerOutcome = commandExecutionTracker.finish(CommandExecutionOutcome.SUCCESS)

        expectThat(nestedOutcome) isEqualTo CommandExecutionOutcome.FAILURE
        expectThat(outerOutcome) isEqualTo CommandExecutionOutcome.FAILURE
    }
}
