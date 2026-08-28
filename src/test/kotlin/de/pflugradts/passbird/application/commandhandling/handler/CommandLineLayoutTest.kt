package de.pflugradts.passbird.application.commandhandling.handler

import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class CommandLineLayoutTest {

    private val canPrintInfo = CanPrintInfo()

    @Test
    fun `should terminate command info rows with exactly one newline`() {
        val outputs = with(canPrintInfo) {
            commandInfoOutputs("f?", "(help)", "Displays this help menu for Favorite commands.")
        }

        expectThat(outputs[2].shell.asString()).isEqualTo("Displays this help menu for Favorite commands.\n")
    }
}
