package de.pflugradts.passbird.application.commandhandling.protein

import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.protein.GuidedSetProteinCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.eventhandling.ProteinEventOutputControl
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

@Tag(INTEGRATION)
class GuidedSetProteinCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val commandHandler = GuidedSetProteinCommandHandler(
        configuration,
        passwordService,
        userInterfaceAdapterPort,
        ProteinEventOutputControl(),
        commandExecutionTracker,
    )
    private val inputHandler = createInputHandlerFor(commandHandler, commandExecutionTracker)

    @Test
    fun `should handle guided set protein command without template`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("0")),
                inputOf(shellOf("url")),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
            ),
            withTheseSecureInputs = listOf(inputOf(shellOf("example.com"))),
        )
        val proteinEntries = mutableListOf<Triple<Slot, String, String>>()
        val outputs = mutableListOf<Output>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries += secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map {
                Triple(it.slot, it.typeShell.asString(), it.structureShell.asString())
            }
            success(Unit)
        }
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(1)
        expectThat(proteinEntries.first().first) isEqualTo DEFAULT
        expectThat(proteinEntries.first().second) isEqualTo "url"
        expectThat(proteinEntries.first().third) isEqualTo "example.com"
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains "Proteins for egg 'EggId' successfully updated."
    }

    @Test
    fun `should abort guided set protein command when template selection is empty`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration, withProteinTemplates = listOf(template("web-login", 0 to "domain")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(emptyShell())),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains "Operation aborted."
    }

    @Test
    fun `should abort guided set protein command when selected template does not exist`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration, withProteinTemplates = listOf(template("web-login", 0 to "domain")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("2"))),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains
            "Specified template does not exist - Operation aborted."
    }

    @Test
    fun `should abort guided set protein command when selected template is empty`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration, withProteinTemplates = listOf(template("empty-template")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains "Specified template is empty - Operation aborted."
    }

    @Test
    fun `should report when no proteins were updated in none mode`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("0")),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
            ),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains
            "No Proteins were updated for egg 'EggId'."
    }

    @Test
    fun `should keep existing structure when type changes and structure is empty in none mode`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(DEFAULT to ShellPair(shellOf("old"), shellOf("secret"))),
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("0")),
                inputOf(shellOf("new")),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
            ),
            withTheseSecureInputs = listOf(inputOf(emptyShell())),
        )
        val proteinEntries = mutableListOf<Pair<String, String>>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries += secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map {
                it.typeShell.asString() to it.structureShell.asString()
            }
            success(Unit)
        }

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(1)
        expectThat(proteinEntries.first().first) isEqualTo "new"
        expectThat(proteinEntries.first().second) isEqualTo "secret"
    }

    @Test
    fun `should keep existing type when entering only a new structure in none mode`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(DEFAULT to ShellPair(shellOf("url"), shellOf("old.example"))),
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("0")),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
            ),
            withTheseSecureInputs = listOf(inputOf(shellOf("new.example"))),
        )
        val proteinEntries = mutableListOf<Pair<String, String>>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries += secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map {
                it.typeShell.asString() to it.structureShell.asString()
            }
            success(Unit)
        }

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(1)
        expectThat(proteinEntries.first().first) isEqualTo "url"
        expectThat(proteinEntries.first().second) isEqualTo "new.example"
    }

    @Test
    fun `should handle guided set protein command with secure protein input not enabled`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration, withSecureProteinInputEnabled = false)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("0")),
                inputOf(shellOf("url")),
                inputOf(shellOf("example.com")),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
            ),
        )
        val proteinEntries = mutableListOf<String>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries += secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map {
                it.structureShell.asString()
            }
            success(Unit)
        }

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(1)
        expectThat(proteinEntries.first()) isEqualTo "example.com"
    }

    @Test
    fun `should handle guided set protein command with secure input toggle enabled and yes response`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(
            instance = configuration,
            withSecureInputEnabled = true,
            withSecureProteinInputEnabled = true,
            withPromptForProteinStructureInputToggle = true,
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("0")),
                inputOf(shellOf("url")),
                inputOf(shellOf("example.com")),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
                inputOf(emptyShell()),
            ),
            withReceiveYes = true,
        )
        val proteinEntries = mutableListOf<String>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries += secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map {
                it.structureShell.asString()
            }
            success(Unit)
        }

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(1)
        expectThat(proteinEntries.first()) isEqualTo "example.com"
    }

    @Test
    fun `should abort guided set protein command when secure input is unavailable`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        val typeShell = spyk(shellOf("url"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("0")),
                inputOf(typeShell),
            ),
        )
        every { userInterfaceAdapterPort.receiveSecurely(any()) } throws SecureInputUnavailableException()
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        verify(exactly = 1) { typeShell.scramble() }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains "Operation aborted."
    }

    @Test
    fun `should handle guided set protein command with template`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(
            instance = configuration,
            withProteinTemplates = listOf(template("web-login", 1 to "user", 4 to "description")),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withTheseSecureInputs = listOf(inputOf(shellOf("alice")), inputOf(emptyShell())),
        )
        val proteinEntries = mutableListOf<Triple<Slot, String, String>>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries += secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map {
                Triple(it.slot, it.typeShell.asString(), it.structureShell.asString())
            }
            success(Unit)
        }

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(1)
        expectThat(proteinEntries.first().first) isEqualTo S1
        expectThat(proteinEntries.first().second) isEqualTo "user"
        expectThat(proteinEntries.first().third) isEqualTo "alice"
        verify(exactly = 1) { passwordService.putProteins(any(), any()) }
    }

    @Test
    fun `should keep existing templated protein when matching type and empty structure are entered`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(S1 to ShellPair(shellOf("user"), shellOf("alice"))),
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(
            instance = configuration,
            withProteinTemplates = listOf(template("web-login", 1 to "user")),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withTheseSecureInputs = listOf(inputOf(emptyShell())),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains
            "Existing Protein Structure at Slot '1' of Egg 'EggId' will be overwritten if you enter a new value."
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains
            "No Proteins were updated for egg 'EggId'."
    }

    @Test
    fun `should keep existing templated protein when type differs and structure is empty`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(S1 to ShellPair(shellOf("login"), shellOf("alice"))),
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(
            instance = configuration,
            withProteinTemplates = listOf(template("web-login", 1 to "user")),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withTheseSecureInputs = listOf(inputOf(emptyShell())),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains
            "Existing Protein Type and Structure at Slot '1' of Egg 'EggId' will be overwritten if you enter a new value."
    }

    @Test
    fun `should overwrite type and structure in template mode when existing type differs`() {
        val egg = createEggForTesting(
            withEggIdShell = shellOf("EggId"),
            withProteins = mapOf(S1 to ShellPair(shellOf("login"), shellOf("alice"))),
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(
            instance = configuration,
            withProteinTemplates = listOf(template("web-login", 1 to "user")),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withTheseSecureInputs = listOf(inputOf(shellOf("bob"))),
        )
        val proteinEntries = mutableListOf<Triple<Slot, String, String>>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries += secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map {
                Triple(it.slot, it.typeShell.asString(), it.structureShell.asString())
            }
            success(Unit)
        }

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(1)
        expectThat(proteinEntries.first().first) isEqualTo S1
        expectThat(proteinEntries.first().second) isEqualTo "user"
        expectThat(proteinEntries.first().third) isEqualTo "bob"
    }

    @Test
    fun `should process only configured template slots`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(
            instance = configuration,
            withProteinTemplates = listOf(template("web-login", 1 to "user", 4 to "description")),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withTheseSecureInputs = listOf(inputOf(shellOf("alice")), inputOf(shellOf("main account"))),
        )
        val proteinEntries = mutableListOf<Slot>()
        every { passwordService.putProteins(shellOf("EggId"), any()) } answers {
            proteinEntries.addAll(secondArg<List<de.pflugradts.passbird.domain.service.password.ProteinEntry>>().map { it.slot })
            success(Unit)
        }

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        expectThat(proteinEntries).hasSize(2)
        expectThat(proteinEntries.toList()).isEqualTo(listOf(S1, S4))
    }

    @Test
    fun `should accept invalid template index format as abort`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(instance = configuration, withProteinTemplates = listOf(template("web-login", 1 to "user")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("01"))),
        )
        val outputs = mutableListOf<Output>()
        every { userInterfaceAdapterPort.send(capture(outputs)) } returns Unit

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
        expectThat(outputs.map { it.shell.asString() }.joinToString("\n")) contains "Operation aborted."
    }

    @Test
    fun `should not write templated proteins when every structure is empty`() {
        val egg = createEggForTesting(withEggIdShell = shellOf("EggId"))
        fakePasswordService(instance = passwordService, withEggs = listOf(egg))
        fakeConfiguration(
            instance = configuration,
            withProteinTemplates = listOf(template("web-login", 1 to "user", 4 to "description")),
        )
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withTheseSecureInputs = listOf(inputOf(emptyShell()), inputOf(emptyShell())),
        )

        inputHandler.handleInput(inputOf(shellOf("p+EggId")))

        verify(exactly = 0) { passwordService.putProteins(any(), any()) }
    }
}

private fun template(name: String, vararg slots: Pair<Int, String>) = Configuration.ProteinTemplate(name).apply {
    slots.forEach { putSlot(it.first.toString(), it.second) }
}
