package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.handler.SetInfoCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

@Tag(INTEGRATION)
class SetInfoCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val setInfoCommandHandler = SetInfoCommandHandler(CanPrintInfo(), configuration, commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(setInfoCommandHandler)

    @Test
    fun `should print info`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "Available Set commands"
    }

    @Test
    fun `should print default`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withPasswordLength = 13,
            withSpecialCharacters = false,
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "13 characters"
        expectThat(captureSystemOut.capture) contains "no special characters"
    }

    @Test
    fun `should list custom configurations`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(
                Configuration.CustomPasswordConfiguration(name = "foo"),
                Configuration.CustomPasswordConfiguration(name = "bar"),
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "1: foo"
        expectThat(captureSystemOut.capture) contains "2: bar"
    }

    @Test
    fun `should print no numbers property`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(Configuration.CustomPasswordConfiguration(name = "foo", hasNumbers = false)),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "no numbers"
    }

    @Test
    fun `should print no lowercase property`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(Configuration.CustomPasswordConfiguration(name = "foo", hasLowercaseLetters = false)),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "no lowercase letters"
    }

    @Test
    fun `should print no uppercase property`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(Configuration.CustomPasswordConfiguration(name = "foo", hasUppercaseLetters = false)),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "no uppercase letters"
    }

    @Test
    fun `should print no letters property`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(
                Configuration.CustomPasswordConfiguration(name = "foo", hasLowercaseLetters = false, hasUppercaseLetters = false),
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "no letters"
    }

    @Test
    fun `should print no special property`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(
                Configuration.CustomPasswordConfiguration(name = "foo", hasSpecialCharacters = false),
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "no special characters"
    }

    @Test
    fun `should print unused special characters property`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(
                Configuration.CustomPasswordConfiguration(name = "foo", hasSpecialCharacters = true, unusedSpecialCharacters = ": \"`\$"),
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "unused special characters: : \"`\$"
    }

    @Test
    fun `should not print unused special characters property if special characters are disabled`() {
        // given
        val input = inputOf(shellOf("s?"))
        fakeConfiguration(
            instance = configuration,
            withCustomPasswordConfigurations = listOf(
                Configuration.CustomPasswordConfiguration(name = "foo", hasSpecialCharacters = false, unusedSpecialCharacters = ": \"`\$"),
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture).not().contains("unused special characters: : \"`\$")
    }
}
