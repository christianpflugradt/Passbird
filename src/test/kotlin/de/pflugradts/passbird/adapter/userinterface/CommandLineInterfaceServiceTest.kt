package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemOut
import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.mockSystemInWith
import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.process.inactivity.InactivityTerminationSignal
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class CommandLineInterfaceServiceTest {

    private val terminalInputGateway = mockk<TerminalInputGateway>()
    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(terminalInputGateway, configuration)

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration)
        every { terminalInputGateway.isConsoleAvailable } returns true
        every { terminalInputGateway.readCharFromStdin() } answers { System.`in`.read().toChar() }
        every { terminalInputGateway.readPasswordFromConsole() } returns CharArray(0)
    }

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

        @Test
        fun `should scramble copied output shell after sending`() {
            // given
            val givenMessage = "hello world"
            val outputShell = spyk(shellOf(givenMessage))
            val renderedShell = spyk(shellOf(givenMessage))
            every { outputShell.copy() } returns renderedShell
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.send(outputOf(outputShell)) }

            // then
            expectThat(captureSystemOut.capture) isEqualTo givenMessage + System.lineSeparator()
            expectThat(outputShell) isEqualTo shellOf(givenMessage)
            verify(exactly = 0) { renderedShell.iterator() }
            verify(exactly = 1) { renderedShell.scramble() }
        }

        @Test
        fun `should update ephemeral line and finish it`() {
            val captureSystemOut = captureSystemOut()

            captureSystemOut.during {
                commandLineInterfaceService.startEphemeralLine(outputOf(shellOf("123456 (30s)")))
                commandLineInterfaceService.updateEphemeralLine(outputOf(shellOf("654321 (29s)")))
                commandLineInterfaceService.finishEphemeralLine()
            }

            expectThat(captureSystemOut.capture).isEqualTo("123456 (30s)\r654321 (29s)\n")
        }

        @Test
        fun `should clear trailing characters when updating ephemeral line with shorter output`() {
            val captureSystemOut = captureSystemOut()

            captureSystemOut.during {
                commandLineInterfaceService.startEphemeralLine(outputOf(shellOf("123456 (30s)")))
                commandLineInterfaceService.updateEphemeralLine(outputOf(shellOf("1 (9s)")))
                commandLineInterfaceService.finishEphemeralLine()
            }

            expectThat(captureSystemOut.capture).isEqualTo("123456 (30s)\r1 (9s)      \r1 (9s)\n")
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

        @ParameterizedTest
        @ValueSource(
            strings = [
                "hello world",
                "n1",
                "semail",
            ],
        )
        fun `should receive input with windows line ending`(givenInput: String) {
            // when
            val actual = mockSystemInWith("$givenInput\r\n") { commandLineInterfaceService.receive() }

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

        @Test
        fun `should not consume input beyond the submitted line`() {
            // given
            val nextPromptInputRead = CountDownLatch(1)
            var readCount = 0
            every { terminalInputGateway.readCharFromStdin() } answers {
                when (++readCount) {
                    1 -> 'c'

                    2 -> '\n'

                    else -> {
                        nextPromptInputRead.countDown()
                        Char.MAX_VALUE
                    }
                }
            }

            // when
            val actual = commandLineInterfaceService.receive()

            // then
            expectThat(actual.shell.asString()) isEqualTo "c"
            expectThat(nextPromptInputRead.await(100, TimeUnit.MILLISECONDS)).isFalse()
            verify(exactly = 2) { terminalInputGateway.readCharFromStdin() }
        }

        @Test
        fun `should abort input when inactivity termination is requested`() {
            // given
            val inactivityTerminationSignal = InactivityTerminationSignal()
            val commandLineInterfaceService =
                CommandLineInterfaceService(terminalInputGateway, configuration, inactivityTerminationSignal)
            every { terminalInputGateway.readCharFromStdin() } answers {
                inactivityTerminationSignal.request()
                Thread.sleep(100)
                Char.MAX_VALUE
            }

            // when / then
            assertThrows<InactivityTerminationRequestedException> { commandLineInterfaceService.receive() }
        }

        @Test
        fun `should abort input when stdin is exhausted before any character is read`() {
            // given
            every { terminalInputGateway.readCharFromStdin() } returns Char.MAX_VALUE

            // when / then
            assertThrows<StdinTerminationRequestedException> { commandLineInterfaceService.receive() }
        }

        @Test
        fun `should abort input when stdin read fails`() {
            // given
            every { terminalInputGateway.readCharFromStdin() } throws IOException()

            // when / then
            assertThrows<StdinTerminationRequestedException> { commandLineInterfaceService.receive() }
        }

        @Test
        fun `should return buffered input when stdin is exhausted without a trailing newline`() {
            // given
            val stdin = listOf('q', Char.MAX_VALUE).iterator()
            every { terminalInputGateway.readCharFromStdin() } answers { stdin.next() }

            // when
            val actual = commandLineInterfaceService.receive()

            // then
            expectThat(actual.shell.asString()) isEqualTo "q"
        }

        @Test
        fun `should detect line break within timeout after intermediate characters`() {
            val stdin = listOf('x', '\n').iterator()
            every { terminalInputGateway.readCharFromStdin() } answers { stdin.next() }

            val actual = commandLineInterfaceService.receiveLineBreakWithin(100L)

            expectThat(actual).isTrue()
        }

        @Test
        fun `should detect carriage return as completed line break within timeout`() {
            every { terminalInputGateway.readCharFromStdin() } returns '\r'

            val actual = commandLineInterfaceService.receiveLineBreakWithin(100L)

            expectThat(actual).isTrue()
        }

        @Test
        fun `should detect stdin exhaustion as completed line break within timeout`() {
            every { terminalInputGateway.readCharFromStdin() } returns Char.MAX_VALUE

            val actual = commandLineInterfaceService.receiveLineBreakWithin(100L)

            expectThat(actual).isTrue()
        }

        @Test
        fun `should return false when no line break is received within timeout`() {
            every { terminalInputGateway.readCharFromStdin() } answers {
                Thread.sleep(200)
                'x'
            }

            val actual = commandLineInterfaceService.receiveLineBreakWithin(50L)

            expectThat(actual).isFalse()
        }

        @Test
        fun `should reuse pending stdin read after timeout while waiting for line break`() {
            val readStarted = CountDownLatch(1)
            val releaseRead = CountDownLatch(1)
            var readCount = 0
            every { terminalInputGateway.readCharFromStdin() } answers {
                when (++readCount) {
                    1 -> {
                        readStarted.countDown()
                        releaseRead.await(1, TimeUnit.SECONDS)
                        '\n'
                    }

                    else -> {
                        Thread.sleep(200)
                        'x'
                    }
                }
            }

            val firstAttempt = commandLineInterfaceService.receiveLineBreakWithin(50L)
            expectThat(firstAttempt).isFalse()
            expectThat(readStarted.await(100, TimeUnit.MILLISECONDS)).isTrue()

            releaseRead.countDown()

            val secondAttempt = commandLineInterfaceService.receiveLineBreakWithin(200L)

            expectThat(secondAttempt).isTrue()
            expectThat(readCount).isEqualTo(1)
        }

        @Test
        fun `should abort waiting for line break when inactivity termination is requested`() {
            val inactivityTerminationSignal = InactivityTerminationSignal().also { it.request() }
            val commandLineInterfaceService =
                CommandLineInterfaceService(terminalInputGateway, configuration, inactivityTerminationSignal)

            assertThrows<InactivityTerminationRequestedException> {
                commandLineInterfaceService.receiveLineBreakWithin(100L)
            }
        }
    }

    @Nested
    inner class ReceiveSecurelyTest {
        @Test
        fun `should receive input securely`() {
            // given
            val givenInput = "hello world"
            every { terminalInputGateway.readPasswordFromConsole() } returns givenInput.toCharArray()
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)

            // when
            val actual = commandLineInterfaceService.receiveSecurely()

            // then
            verify(exactly = 1) { terminalInputGateway.readPasswordFromConsole() }
            expectThat(actual.shell.asString()) isEqualTo givenInput
        }

        @Test
        fun `should clear original console char array after secure input conversion`() {
            // given
            val givenInput = "hello world"
            val consoleInput = givenInput.toCharArray()
            every { terminalInputGateway.readPasswordFromConsole() } returns consoleInput
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)

            // when
            val actual = commandLineInterfaceService.receiveSecurely()

            // then
            expectThat(actual.shell.asString()) isEqualTo givenInput
            expectThat(consoleInput.toList()) isEqualTo List(givenInput.length) { Char.MIN_VALUE }
        }

        @Test
        fun `should receive input securely when sending output`() {
            // given
            val givenMessage = "hello world"
            every { terminalInputGateway.readPasswordFromConsole() } returns "smth".toCharArray()
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandLineInterfaceService.receiveSecurely(outputOf(shellOf(givenMessage))) }

            // then
            verify(exactly = 1) { terminalInputGateway.readPasswordFromConsole() }
            expectThat(captureSystemOut.capture) isEqualTo givenMessage
        }

        @Test
        fun `should abort secure input when inactivity termination is requested`() {
            // given
            val inactivityTerminationSignal = InactivityTerminationSignal()
            val commandLineInterfaceService =
                CommandLineInterfaceService(terminalInputGateway, configuration, inactivityTerminationSignal)
            every { terminalInputGateway.isConsoleAvailable } returns true
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)
            every { terminalInputGateway.readPasswordFromConsole() } answers {
                inactivityTerminationSignal.request()
                Thread.sleep(100)
                "secret".toCharArray()
            }

            // when / then
            assertThrows<InactivityTerminationRequestedException> { commandLineInterfaceService.receiveSecurely() }
            verify(exactly = 1) { terminalInputGateway.readPasswordFromConsole() }
        }

        @Test
        fun `should abort secure input before reading when inactivity termination is already requested`() {
            // given
            val inactivityTerminationSignal = InactivityTerminationSignal()
            val commandLineInterfaceService =
                CommandLineInterfaceService(terminalInputGateway, configuration, inactivityTerminationSignal)
            inactivityTerminationSignal.request()
            every { terminalInputGateway.isConsoleAvailable } returns true
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)

            // when / then
            assertThrows<InactivityTerminationRequestedException> { commandLineInterfaceService.receiveSecurely() }
            verify(exactly = 0) { terminalInputGateway.readPasswordFromConsole() }
        }

        @Test
        fun `should keep waiting for secure input while inactivity termination is not requested`() {
            // given
            val inactivityTerminationSignal = InactivityTerminationSignal()
            val commandLineInterfaceService =
                CommandLineInterfaceService(terminalInputGateway, configuration, inactivityTerminationSignal)
            every { terminalInputGateway.isConsoleAvailable } returns true
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)
            every { terminalInputGateway.readPasswordFromConsole() } answers {
                Thread.sleep(100)
                "secret".toCharArray()
            }

            // when
            val actual = commandLineInterfaceService.receiveSecurely()

            // then
            expectThat(actual.shell.asString()) isEqualTo "secret"
            verify(exactly = 1) { terminalInputGateway.readPasswordFromConsole() }
        }

        @Test
        fun `should receive secure input as plain if secure input is disabled`() {
            // given
            val givenInput = "hello world"
            fakeConfiguration(instance = configuration, withSecureInputEnabled = false)

            // when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveSecurely() }

            // then
            verify(exactly = 0) { terminalInputGateway.readPasswordFromConsole() }
            expectThat(actual.shell.asString()) isEqualTo givenInput
        }

        @Test
        fun `should abort secure input if console is unavailable`() {
            // given
            every { terminalInputGateway.isConsoleAvailable } returns false
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)

            // when
            assertThrows<SecureInputUnavailableException> { commandLineInterfaceService.receiveSecurely() }

            // then
            verify(exactly = 0) { terminalInputGateway.readPasswordFromConsole() }
        }

        @Test
        fun `should abort secure input after sending output if console is unavailable`() {
            // given
            val givenMessage = "hello world"
            every { terminalInputGateway.isConsoleAvailable } returns false
            fakeConfiguration(instance = configuration, withSecureInputEnabled = true)
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during {
                assertThrows<SecureInputUnavailableException> {
                    commandLineInterfaceService.receiveSecurely(outputOf(shellOf(givenMessage)))
                }
            }

            // then
            verify(exactly = 0) { terminalInputGateway.readPasswordFromConsole() }
            expectThat(captureSystemOut.capture) isEqualTo givenMessage
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

        @Test
        fun `should return true on input c with windows line ending`() {
            // given
            val givenInput = "c"

            // when
            val actual = mockSystemInWith("$givenInput\r\n") { commandLineInterfaceService.receiveConfirmation(emptyOutput()) }

            // then
            expectThat(actual).isTrue()
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "cc",
                "c1",
                "c!",
                "c ",
                "c\t",
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
        @ParameterizedTest
        @ValueSource(
            strings = [
                "Y",
                "y",
            ],
        )
        fun `should return true on yes input`(givenInput: String) {
            // given / when
            val actual = mockSystemInWith("$givenInput\n") { commandLineInterfaceService.receiveYes(emptyOutput()) }

            // then
            expectThat(actual).isTrue()
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "Y",
                "y",
            ],
        )
        fun `should return true on yes input with windows line ending`(givenInput: String) {
            // given / when
            val actual = mockSystemInWith("$givenInput\r\n") { commandLineInterfaceService.receiveYes(emptyOutput()) }

            // then
            expectThat(actual).isTrue()
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "n",
                "Yes",
                "y1",
                "Y!",
                "y ",
                "Y\t",
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
