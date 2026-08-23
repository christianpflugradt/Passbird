package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.passbird.application.commandhandling.command.GuidedSetProteinCommand
import de.pflugradts.passbird.application.commandhandling.command.SetProteinCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA

class ProteinCommandFactoryTest {

    private val proteinCommandFactory = ProteinCommandFactory()

    @Test
    fun `should construct guided set protein command for add variant without slot`() {
        val actual = proteinCommandFactory.constructFromInput(inputOf(shellOf("p+EggId")))

        expectThat(actual).isA<GuidedSetProteinCommand>()
    }

    @Test
    fun `should construct slotted set protein command for add variant with slot`() {
        val actual = proteinCommandFactory.constructFromInput(inputOf(shellOf("p+0EggId")))

        expectThat(actual).isA<SetProteinCommand>()
    }
}
