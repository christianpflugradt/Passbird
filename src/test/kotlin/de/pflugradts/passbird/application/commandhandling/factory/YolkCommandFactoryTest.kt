package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.passbird.application.commandhandling.command.DiscardYolkCommand
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.SetYolkCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewYolkCommand
import de.pflugradts.passbird.application.commandhandling.command.YolkInfoCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA

class YolkCommandFactoryTest {

    private val yolkCommandFactory = YolkCommandFactory()

    @Test
    fun `should construct yolk info commands`() {
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("y")))).isA<YolkInfoCommand>()
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("y?")))).isA<YolkInfoCommand>()
    }

    @Test
    fun `should construct yolk data commands`() {
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("yEggId")))).isA<ViewYolkCommand>()
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("y+EggId")))).isA<SetYolkCommand>()
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("y-EggId")))).isA<DiscardYolkCommand>()
    }

    @Test
    fun `should reject invalid yolk command variants`() {
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("y!")))).isA<NullCommand>()
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("y*EggId")))).isA<NullCommand>()
        expectThat(yolkCommandFactory.constructFromInput(inputOf(shellOf("y??")))).isA<NullCommand>()
    }
}
