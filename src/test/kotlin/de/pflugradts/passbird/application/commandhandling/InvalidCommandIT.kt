package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class InvalidCommandIT {

    @Nested
    inner class InputHandlerIT {
        private val commandBus = spyk(CommandBus(emptySet()))
        private val inputHandler = createInputHandlerFor(commandBus)

        @Test
        fun `should handle unknown command`() {
            // given
            val input = inputOf(bytesOf("?"))

            // when
            inputHandler.handleInput(input)

            // then
            verify { commandBus.post(any(NullCommand::class)) }
        }

        @Test
        fun `should handle empty command`() {
            // given
            val input = inputOf(emptyBytes())

            // when
            inputHandler.handleInput(input)

            // then
            verify { commandBus.post(any(NullCommand::class)) }
        }
    }

    @Nested
    inner class CommandFactoryIT {
        private val commandFactory = CommandFactory(NamespaceCommandFactory())

        @ParameterizedTest
        @ValueSource(
            strings = [
                "n+11",
                "n1234",
                "n---",
                "n(3)",
            ],
        )
        fun `should handle namespace command with too large command`(givenInput: String) {
            // given
            val input = inputOf(bytesOf(givenInput))

            // when
            ByteArrayOutputStream().use { stream ->
                PrintStream(stream).use { printStream ->
                    System.setErr(printStream)
                    val actual = commandFactory.construct(CommandType.resolveCommandTypeFrom(input.command), input)
                    val errorOutput = String(stream.toByteArray())

                    // then
                    expectThat(actual).isA<NullCommand>()
                    expectThat(errorOutput) contains "Command execution failed:"
                }
            }
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "n*1alias",
                "n-1alias",
                "n/1alias",
                "n()alias",
                "n_alias",
                "n++",
            ],
        )
        fun `should handle unknown namespace command`(givenInput: String) {
            // given
            val input = inputOf(bytesOf(givenInput))

            // when
            val actual = commandFactory.construct(CommandType.resolveCommandTypeFrom(input.command), input)

            // then
            expectThat(actual).isA<NullCommand>()
        }
    }
}
