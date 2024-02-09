package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemOut
import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.mockSystemInWith
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.stream.Stream

class CommandLineInterfaceServiceTest {

    private val systemOperation = mockk<SystemOperation>()
    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(systemOperation, configuration)

    @BeforeEach
    fun setup() { fakeConfiguration(instance = configuration) }

    @Nested
    inner class SendTest {
        @Test
        fun `should send output`() {
            // given
            val givenMessage = "hello world"
            val expectedMessage = givenMessage + System.lineSeparator()
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.send(outputOf(shellOf(givenMessage))) }

            // then
            expectThat(captureSystemOut.capture) isEqualTo expectedMessage
        }

        @Test
        fun `should send line break`() {
            // given
            val expectedMessage = System.lineSeparator()
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.sendLineBreak() }

            // then
            expectThat(captureSystemOut.capture) isEqualTo expectedMessage
        }
    }

    @Nested
    inner class ReceiveTest {
        @Test
        fun `should receive input`() {
            // given
            val givenInput = "hello world"

            // when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receive() }

            // then
            expectThat(actual.shell.asString()) isEqualTo givenInput
        }

        @Test
        fun `should receive input when sending output`() {
            // given
            val givenMessage = "hello world"
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during {
                mockSystemInWith("smth\n") {
                    commandLineInterfaceService.receive(outputOf(shellOf(givenMessage)))
                }
            }

            // then
            expectThat(captureSystemOut.capture) isEqualTo givenMessage
        }
    }

    @Nested
    inner class ReceiveSecurelyTest {
        @Test
        fun `should receive input securely`() {
            // given
            val givenInput = "hello world"
            fakeSystemOperation(instance = systemOperation, withPasswordFromConsole = givenInput.toCharArray())
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)

            // when
            val actual = commandLineInterfaceService.receiveSecurely()

            // then
            verify(exactly = 1) { systemOperation.readPasswordFromConsole() }
            expectThat(actual.shell.asString()) isEqualTo givenInput
        }

        @Test
        fun `should receive input securely when sending output`() {
            // given
            val givenMessage = "hello world"
            fakeSystemOperation(instance = systemOperation, withPasswordFromConsole = "smth".toCharArray())
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.receiveSecurely(outputOf(shellOf(givenMessage))) }

            // then
            verify(exactly = 1) { systemOperation.readPasswordFromConsole() }
            expectThat(captureSystemOut.capture) isEqualTo givenMessage
        }

        @Test
        fun `should receive secure input as plain if secure input is disabled`() {
            // given
            val givenInput = "hello world"
            fakeConfiguration(instance = configuration, withSecureInputEnabled = false)

            // when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveSecurely() }

            // then
            verify(exactly = 0) { systemOperation.readPasswordFromConsole() }
            expectThat(actual.shell.asString()) isEqualTo givenInput
        }

        @Test
        fun `should receive secure input as plain if console is unavailable`() {
            // given
            val givenInput = "hello world"
            fakeSystemOperation(instance = systemOperation, withConsoleEnabled = false)
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)

            // when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveSecurely() }

            // then
            verify(exactly = 0) { systemOperation.readPasswordFromConsole() }
            expectThat(actual.shell.asString()) isEqualTo givenInput
        }
    }

    @Nested
    inner class ReceiveConfirmationTest {
        @Test
        fun `should return true on input c`() {
            // given
            val givenInput = "c"

            // when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveConfirmation(emptyOutput()) }

            // then
            expectThat(actual).isTrue()
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "cc",
                "d",
                "",
            ],
        )
        fun `should return false on other input`(givenInput: String) {
            // given / when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveConfirmation(emptyOutput()) }

            // then
            expectThat(actual).isFalse()
        }
    }

    @Nested
    inner class ReceiveYesTest {
        @Test
        fun `should return true on input Y`() {
            // given
            val givenInput = "Y"

            // when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveYes(emptyOutput()) }

            // then
            expectThat(actual).isTrue()
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "n",
                "y",
                "Yes",
                "",
            ],
        )
        fun `should return false on other input`(givenInput: String) {
            // given / when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveYes(emptyOutput()) }

            // then
            expectThat(actual).isFalse()
        }
    }

    @Nested
    inner class BellTest {
        @Test
        fun `should send bell character on warning sound if enabled`() {
            // given
            fakeConfiguration(instance = configuration, withAudibleBellEnabled = true)
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.warningSound() }

            // then
            expectThat(captureSystemOut.capture) isEqualTo "\u0007"
        }

        @Test
        fun `should not send bell character on warning sound if disabled`() {
            // given
            fakeConfiguration(instance = configuration, withAudibleBellEnabled = false)
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.warningSound() }

            // then
            expectThat(captureSystemOut.capture) isEqualTo ""
        }

        @Test
        fun `should send bell character on output formatting set to abort operation`() {
            // given
            fakeConfiguration(instance = configuration, withAudibleBellEnabled = true)
            val givenOutput = outputOf(shellOf("foo"), OPERATION_ABORTED)
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.send(givenOutput) }

            // then
            expectThat(captureSystemOut.capture) contains "\u0007"
        }
    }

    @ParameterizedTest
    @MethodSource("providedMapping")
    fun `should send output with escape codes`(outputFormatting: OutputFormatting, code: Int) {
        // given
        val givenMessage = "hello world"
        fakeConfiguration(instance = configuration, withAnsiEscapeCodesEnabled = true)
        val captureSystemOut = captureSystemOut()

        // when
        captureSystemOut.during { commandLineInterfaceService.send(outputOf(shellOf(givenMessage), outputFormatting)) }

        // then
        expectThat(captureSystemOut.capture) isEqualTo "\u001B[38;5;${code}m$givenMessage\u001B[0m\n"
    }

    companion object {
        @JvmStatic
        fun providedMapping(): Stream<Arguments> = Stream.of(
            Arguments.of(OutputFormatting.DEFAULT, 231),
            Arguments.of(OutputFormatting.SPECIAL, 220),
            Arguments.of(OPERATION_ABORTED, 208),
            Arguments.of(OutputFormatting.ERROR_MESSAGE, 196),
            Arguments.of(OutputFormatting.HIGHLIGHT, 207),
            Arguments.of(OutputFormatting.NEST, 39),
            Arguments.of(OutputFormatting.EVENT_HANDLED, 118),
        )
    }
}
