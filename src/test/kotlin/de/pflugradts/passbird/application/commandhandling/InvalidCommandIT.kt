package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
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
import strikt.assertions.isEqualTo

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

        @Test
        fun `should handle single char command with invalid input`() {
            // given
            val givenCommand = "c"
            val givenArgument = "1"
            val input = inputOf(bytesOf("$givenCommand$givenArgument"))
            val captureSystemErr = captureSystemErr()

            // when
            captureSystemErr.during {
                inputHandler.handleInput(input)
            }
            val actual = captureSystemErr.capture

            // then
            expectThat(actual) isEqualTo "Command execution failed: Parameter for command '$givenCommand' not supported: $givenArgument\n"
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
            val captureSystemErr = captureSystemErr()
            val actual = captureSystemErr.during {
                commandFactory.construct(CommandType.resolveCommandTypeFrom(input.command), input)
            }

            // then
            expectThat(actual).isA<NullCommand>()
            expectThat(captureSystemErr.capture) contains "Command execution failed:"
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
