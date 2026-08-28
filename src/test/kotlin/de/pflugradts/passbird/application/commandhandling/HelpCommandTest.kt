package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.egg.TestYolkData
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.NEST
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@Tag(INTEGRATION)
class HelpCommandTest {

    private val configuration = mockk<Configuration>()
    private val nestService = createNestServiceForTesting()
    private val passwordService = mockk<de.pflugradts.passbird.domain.service.password.PasswordService>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val helpCommandHandler = HelpCommandHandler(CanPrintInfo(), nestService, passwordService, commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(helpCommandHandler)

    @Test
    fun `should handle help command`() {
        // given
        val input = Input.inputOf(shellOf("h"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "Usage: [command][parameter]"
        expectThat(captureSystemOut.capture) contains "    k           (keystore)"
        expectThat(captureSystemOut.capture) contains "Lists all EggIds across all Nests, grouped by Nest."
        expectThat(captureSystemOut.capture) contains "e*"
        expectThat(captureSystemOut.capture) contains "    f?          (Favorites)"
        expectThat(captureSystemOut.capture) contains "i*"
        expectThat(captureSystemOut.capture) contains "    .           (repeat)"
        expectThat(captureSystemOut.capture) contains "    s*[EggId]   (set once)"
        expectThat(captureSystemOut.capture) contains
            "    u[EggId]    (use)         Guides through using login, password, and optional Yolk for the specified Egg."
        expectThat(captureSystemOut.capture) contains
            "    h*          (help with stats)   Displays password-tree statistics before this help menu."
        expectThat(captureSystemOut.capture) contains "    d[EggId]    (discard)     Moves the specified Egg to trash."
        expectThat(captureSystemOut.capture) contains "    d![EggId]   (force)       Permanently deletes the specified Egg."
        expectThat(captureSystemOut.capture) contains "    d           (trash)       Displays trashed Eggs and allows restoring them."
        expectThat(captureSystemOut.capture) contains
            "    d![EggId]   (force)       Permanently deletes the specified Egg.\n    d           (trash)       Displays trashed Eggs and allows restoring them."
        expectThat(captureSystemOut.capture) contains
            "    q           (quit)              Exits the Passbird application.\n\n    n           (Nests)             Displays available Nests and related commands."
        expectThat(captureSystemOut.capture).not().contains("\t")
    }

    @Test
    fun `should handle help stats command`() {
        // given
        arrangeHelpStatsData()
        val input = Input.inputOf(shellOf("h*"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains expectedHelpStatsOutput()
        expectThat(captureSystemOut.capture).not().contains("Stats\n")
    }

    @Test
    fun `should render help stats headings in highlight and values in default when escape codes are enabled`() {
        // given
        arrangeHelpStatsData()
        val input = Input.inputOf(shellOf("h*"))
        fakeConfiguration(instance = configuration, withAnsiEscapeCodesEnabled = true)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains escaped("Current Nest\n", HIGHLIGHT)
        expectThat(captureSystemOut.capture) contains escaped("Across All Nests\n", HIGHLIGHT)
        expectThat(captureSystemOut.capture) contains escaped("3\n", OutputFormatting.DEFAULT)
        expectThat(captureSystemOut.capture).not().contains(escaped("3\n", NEST))
    }

    @Test
    fun `should reject help command with trailing input`() {
        // given
        val input = Input.inputOf(shellOf("hhelp"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()
        val captureSystemErr = captureSystemErr()

        // when
        captureSystemErr.during {
            captureSystemOut.during {
                inputHandler.handleInput(input)
            }
        }

        // then
        expectThat(captureSystemOut.capture) isEqualTo ""
        expectThat(captureSystemErr.capture) isEqualTo "Command execution failed: Parameter for command 'h' not supported: help\n"
    }

    private fun arrangeHelpStatsData() {
        nestService.place(shellOf("work"), S1)
        nestService.place(shellOf("private"), S2)
        nestService.moveToNestAt(S1)
        fakePasswordService(
            instance = passwordService,
            withNestService = nestService,
            withNestFavoriteCounts = mapOf(DEFAULT to 1, S1 to 2, S2 to 1),
            withEggs = statsEggs(),
        )
    }

    private fun statsEggs() = listOf(
        createEggForTesting(
            withEggIdShell = shellOf("mail"),
            withSlot = S1,
            withProteins = mapOf(
                DEFAULT to Pair(shellOf("username"), shellOf("alice")),
                S1 to Pair(shellOf("url"), shellOf("mail.example")),
            ),
            withYolk = TestYolkData(shellOf("secret-1")),
        ),
        createEggForTesting(
            withEggIdShell = shellOf("calendar"),
            withSlot = S1,
            withProteins = mapOf(DEFAULT to Pair(shellOf("url"), shellOf("calendar.example"))),
        ),
        createEggForTesting(withEggIdShell = shellOf("notes"), withSlot = S1),
        createEggForTesting(withEggIdShell = shellOf("bank"), withSlot = S2, withYolk = TestYolkData(shellOf("secret-2"))),
        createEggForTesting(
            withEggIdShell = shellOf("shop"),
            withSlot = S2,
            withProteins = mapOf(
                DEFAULT to Pair(shellOf("username"), shellOf("bob")),
                S1 to Pair(shellOf("url"), shellOf("shop.example")),
            ),
            withYolk = TestYolkData(shellOf("secret-3")),
        ),
        createEggForTesting(
            withEggIdShell = shellOf("forum"),
            withSlot = DEFAULT,
            withProteins = mapOf(DEFAULT to Pair(shellOf("username"), shellOf("carol"))),
        ),
    )

    private fun expectedHelpStatsOutput() = """
Current Nest
    Eggs:                   3
    Eggs with Yolks:        1
    Eggs with Proteins:     2
    Occupied Protein Slots: 3
    Assigned Favorites:     2

Across All Nests
    Eggs:                   6
    Active Nests:           2
    Eggs with Yolks:        3
    Eggs with Proteins:     4
    Occupied Protein Slots: 6
    Assigned Favorites:     4

Usage: [command][parameter]
    """.trimIndent()

    private fun escaped(text: String, formatting: OutputFormatting) = when (formatting) {
        OutputFormatting.DEFAULT -> "\u001B[38;5;231m$text\u001B[0m"
        HIGHLIGHT -> "\u001B[38;5;207m$text\u001B[0m"
        NEST -> "\u001B[38;5;39m$text\u001B[0m"
        else -> error("Unsupported formatting in test: $formatting")
    }
}
