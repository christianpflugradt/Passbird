package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Tag(INTEGRATION)
class InvalidCommandTest {

    @Nested
    inner class InputHandlerTest {
        private val commandBus = spyk(CommandBus(emptySet()))
        private val inputHandler = createInputHandlerFor(commandBus)

        @Test
        fun `should handle unknown command`() {
            // given
            val input = inputOf(shellOf("z"))

            // when
            inputHandler.handleInput(input)

            // then
            verify { commandBus.post(any(NullCommand::class)) }
        }

        @Test
        fun `should handle empty command`() {
            // given
            val input = inputOf(emptyShell())

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
            val input = inputOf(shellOf("$givenCommand$givenArgument"))
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
    inner class CommandFactoryTest {
        private val commandFactory = CommandFactory(NestCommandFactory(), SetCommandFactory())

        @ParameterizedTest
        @ValueSource(
            strings = [
                "n+11",
                "n1234",
                "n---",
                "n(3)",
            ],
        )
        fun `should handle nest command with too large command`(givenInput: String) {
            // given
            val input = inputOf(shellOf(givenInput))

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
                "n*1eggId",
                "n+1eggId",
                "n-1eggId",
                "n()eggId",
                "n+",
                "n1+",
                "n++",
                "n--",
            ],
        )
        fun `should handle unknown nest command`(givenInput: String) {
            // given
            val input = inputOf(shellOf(givenInput))

            // when
            val actual = commandFactory.construct(CommandType.resolveCommandTypeFrom(input.command), input)

            // then
            expectThat(actual).isA<NullCommand>()
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "s+34",
                "s1234",
                "s---",
                "s(5)",
            ],
        )
        fun `should handle set command with too large command`(givenInput: String) {
            // given
            val input = inputOf(shellOf(givenInput))

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
                "s",
                "s1",
                "s?1",
                "s?eggId",
                "s?3eggId",
                "s-1eggId",
                "s++",
            ],
        )
        fun `should handle unknown set command`(givenInput: String) {
            // given
            val input = inputOf(shellOf(givenInput))

            // when
            val actual = commandFactory.construct(CommandType.resolveCommandTypeFrom(input.command), input)

            // then
            expectThat(actual).isA<NullCommand>()
        }
    }
}
