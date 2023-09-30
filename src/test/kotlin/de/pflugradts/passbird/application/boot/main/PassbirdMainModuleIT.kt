package de.pflugradts.passbird.application.boot.main

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.util.Modules
import de.pflugradts.passbird.application.commandhandling.handler.CustomSetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.DiscardCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.GetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.RenameCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.SetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ViewCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AddNamespaceCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AssignNamespaceCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.namespace.SwitchNamespaceCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.namespace.ViewNamespaceCommandHandler
import de.pflugradts.passbird.application.eventhandling.ApplicationEventHandler
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isSameInstanceAs

internal class PassbirdMainModuleIT {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val actual = Guice.createInjector(Modules.override(ApplicationModule()).with(PassbirdTestModule()))
            .getInstance(PassbirdTestMain::class.java)

        // then
        expectThat(actual.bootable).isA<PassbirdApplication>()
        val expectedCommandHandlers = listOf(
            AddNamespaceCommandHandler::class.java,
            AssignNamespaceCommandHandler::class.java,
            CustomSetCommandHandler::class.java,
            DiscardCommandHandler::class.java,
            ExportCommandHandler::class.java,
            GetCommandHandler::class.java,
            HelpCommandHandler::class.java,
            ImportCommandHandler::class.java,
            ListCommandHandler::class.java,
            QuitCommandHandler::class.java,
            RenameCommandHandler::class.java,
            SetCommandHandler::class.java,
            SwitchNamespaceCommandHandler::class.java,
            ViewCommandHandler::class.java,
            ViewNamespaceCommandHandler::class.java,
        )
        actual.commandHandlers.forEachIndexed { index, commandHandler ->
            expectThat(commandHandler::class.java) isSameInstanceAs expectedCommandHandlers[index]
        }
        val expectedEventHandlers = listOf(
            ApplicationEventHandler::class.java,
            DomainEventHandler::class.java,
        )
        actual.eventHandlers.forEachIndexed { index, eventHandler ->
            expectThat(eventHandler::class.java) isSameInstanceAs expectedEventHandlers[index]
        }
    }

    internal class PassbirdTestModule : AbstractModule() {
        public override fun configure() { bind(CryptoProvider::class.java).toInstance(Mockito.mock(CryptoProvider::class.java)) }
    }
}
