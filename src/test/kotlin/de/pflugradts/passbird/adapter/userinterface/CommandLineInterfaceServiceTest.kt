package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

internal class CommandLineInterfaceServiceTest {

    private val systemOperation = mockk<SystemOperation>()
    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(systemOperation, configuration)

    @Nested
    internal inner class SendTest {
        @Test
        fun `should send output`() {
            // given
            val givenMessage = "hello world"
            val expectedMessage = givenMessage + System.lineSeparator()
            var actual: String
            ByteArrayOutputStream().use { stream ->
                PrintStream(stream).use { printStream ->
                    System.setOut(printStream)

                    // when
                    commandLineInterfaceService.send(Output.of(Bytes.bytesOf(givenMessage)))

                    // then
                    actual = String(stream.toByteArray())
                }
            }
            expectThat(actual) isEqualTo expectedMessage
        }

        @Test
        fun `should send line break`() {
            // given
            val expectedMessage = System.lineSeparator()
            var actual: String
            ByteArrayOutputStream().use { stream ->
                PrintStream(stream).use { printStream ->
                    System.setOut(printStream)

                    // when
                    commandLineInterfaceService.sendLineBreak()

                    // then
                    actual = String(stream.toByteArray())
                }
            }
            expectThat(actual) isEqualTo expectedMessage
        }
    }

    @Nested
    internal inner class ReceiveTest {
        @Test
        fun `should receive input`() {
            // given
            val givenInput = "hello world"
            val inputBytes = (givenInput + System.lineSeparator()).toByteArray()
            var actual: Input
            ByteArrayInputStream(inputBytes).use { stream ->
                System.setIn(stream)

                // when
                actual = commandLineInterfaceService.receive()
            }

            // then
            expectThat(actual.bytes.asString()) isEqualTo givenInput
        }

        @Test
        fun `should receive input when sending output`() {
            // given
            val givenMessage = "hello world"
            var actual: String
            ByteArrayOutputStream().use { stream ->
                PrintStream(stream).use { printStream ->
                    ByteArrayInputStream(("smth" + System.lineSeparator()).toByteArray()).use { inStream ->
                        System.setOut(printStream)
                        System.setIn(inStream)

                        // when
                        commandLineInterfaceService.receive(Output.of(Bytes.bytesOf(givenMessage)))

                        // then
                        actual = String(stream.toByteArray())
                    }
                }
            }
            expectThat(actual) isEqualTo givenMessage
        }
    }

    @Nested
    internal inner class ReceiveSecurelyTest {
        @Test
        fun `should receive input securely`() {
            // given
            val givenInput = "hello world"
            fakeSystemOperation(
                instance = systemOperation,
                withPasswordFromConsole = givenInput.toCharArray(),
            )
            fakeConfiguration(
                instance = configuration,
                withSecureInputEnabled = true,
            )

            // when
            val actual = commandLineInterfaceService.receiveSecurely()

            // then
            verify(exactly = 1) { systemOperation.readPasswordFromConsole() }
            expectThat(actual.bytes.asString()) isEqualTo givenInput
        }

        @Test
        fun `should receive input securely when sending output`() {
            // given
            val givenMessage = "hello world"
            fakeSystemOperation(
                instance = systemOperation,
                withPasswordFromConsole = "smth".toCharArray(),
            )
            fakeConfiguration(
                instance = configuration,
                withSecureInputEnabled = true,
            )
            var actual: String
            ByteArrayOutputStream().use { stream ->
                PrintStream(stream).use { printStream ->
                    System.setOut(printStream)

                    // when
                    commandLineInterfaceService.receiveSecurely(Output.of(Bytes.bytesOf(givenMessage)))

                    // then
                    actual = String(stream.toByteArray())
                }
            }
            verify(exactly = 1) { systemOperation.readPasswordFromConsole() }
            expectThat(actual) isEqualTo givenMessage
        }

        @Test
        fun `should receive secure input as plain if secure input is disabled`() {
            // given
            val givenInput = "hello world"
            val inputBytes = (givenInput + System.lineSeparator()).toByteArray()
            fakeConfiguration(
                instance = configuration,
                withSecureInputEnabled = false,
            )
            var actual: Input
            ByteArrayInputStream(inputBytes).use { stream ->
                System.setIn(stream)

                // when
                actual = commandLineInterfaceService.receiveSecurely()
            }

            // then
            verify(exactly = 0) { systemOperation.readPasswordFromConsole() }
            expectThat(actual.bytes.asString()) isEqualTo givenInput
        }

        @Test
        fun `should receive secure input as plain if console is unavailable`() {
            // given
            val givenInput = "hello world"
            val inputBytes = (givenInput + System.lineSeparator()).toByteArray()
            fakeSystemOperation(
                instance = systemOperation,
                withConsoleEnabled = false,
            )
            fakeConfiguration(
                instance = configuration,
                withSecureInputEnabled = true,
            )
            var actual: Input
            ByteArrayInputStream(inputBytes).use { stream ->
                System.setIn(stream)

                // when
                actual = commandLineInterfaceService.receiveSecurely()
            }

            // then
            verify(exactly = 0) { systemOperation.readPasswordFromConsole() }
            expectThat(actual.bytes.asString()) isEqualTo givenInput
        }
    }

    @Nested
    internal inner class ReceiveConfirmationTest {
        @Test
        fun `should return true on input c`() {
            // given
            val givenInput = "c"
            val inputBytes = (givenInput + System.lineSeparator()).toByteArray()
            var actual: Boolean
            ByteArrayInputStream(inputBytes).use { stream ->
                System.setIn(stream)

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty())
            }

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should return false on input cc`() {
            // given
            val givenInput = "cc"
            val inputBytes = (givenInput + System.lineSeparator()).toByteArray()
            var actual: Boolean
            ByteArrayInputStream(inputBytes).use { stream ->
                System.setIn(stream)

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty())
            }

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should return false on input d`() {
            // given
            val givenInput = "d"
            val inputBytes = (givenInput + System.lineSeparator()).toByteArray()
            var actual: Boolean
            ByteArrayInputStream(inputBytes).use { stream ->
                System.setIn(stream)

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty())
            }

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should return false on empty input`() {
            // given
            val givenInput = ""
            val inputBytes = (givenInput + System.lineSeparator()).toByteArray()
            var actual: Boolean
            ByteArrayInputStream(inputBytes).use { stream ->
                System.setIn(stream)

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty())
            }

            // then
            expectThat(actual).isFalse()
        }
    }
}
