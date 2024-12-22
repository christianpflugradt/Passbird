package de.pflugradts.passbird.application.boot.main

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.name.Names
import com.google.inject.util.Modules
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.SetInfoCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.CustomSetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.DiscardCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.GetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.RenameCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.SetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.ViewCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.AddNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.DiscardNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.MoveToNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.SwitchNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.nest.ViewNestCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.DiscardProteinCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.GetProteinCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.ProteinInfoCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.SetProteinCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.ViewProteinStructuresCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.protein.ViewProteinTypesCommandHandler
import de.pflugradts.passbird.application.eventhandling.ApplicationEventHandler
import de.pflugradts.passbird.application.process.backup.BackupManager
import de.pflugradts.passbird.application.process.exchange.ExportFileChecker
import de.pflugradts.passbird.application.process.inactivity.InactivityHandlerScheduler
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isSameInstanceAs

@Tag(INTEGRATION)
class PassbirdMainModuleTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val actual = Guice.createInjector(Modules.override(ApplicationModule()).with(PassbirdTestModule()))
            .getInstance(PassbirdTestMain::class.java)

        // then
        expectThat(actual.bootable).isA<PassbirdApplication>()
        val expectedCommandHandlers = listOf(
            AddNestCommandHandler::class.java,
            MoveToNestCommandHandler::class.java,
            CustomSetCommandHandler::class.java,
            DiscardCommandHandler::class.java,
            DiscardNestCommandHandler::class.java,
            DiscardProteinCommandHandler::class.java,
            ExportCommandHandler::class.java,
            GetCommandHandler::class.java,
            GetProteinCommandHandler::class.java,
            HelpCommandHandler::class.java,
            ImportCommandHandler::class.java,
            ListCommandHandler::class.java,
            ProteinInfoCommandHandler::class.java,
            QuitCommandHandler::class.java,
            RenameCommandHandler::class.java,
            SetCommandHandler::class.java,
            SetInfoCommandHandler::class.java,
            SetProteinCommandHandler::class.java,
            SwitchNestCommandHandler::class.java,
            ViewCommandHandler::class.java,
            ViewNestCommandHandler::class.java,
            ViewProteinStructuresCommandHandler::class.java,
            ViewProteinTypesCommandHandler::class.java,
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
        val expectedInitializers = listOf(
            ExportFileChecker::class.java,
            InactivityHandlerScheduler::class.java,
        )
        actual.initializers.forEachIndexed { index, initializer ->
            expectThat(initializer::class.java) isSameInstanceAs expectedInitializers[index]
        }
        val expectedFinalizers = listOf(
            BackupManager::class.java,
        )
        actual.finalizers.forEachIndexed { index, finalizer ->
            expectThat(finalizer::class.java) isSameInstanceAs expectedFinalizers[index]
        }
    }

    class PassbirdTestModule : AbstractModule() {
        public override fun configure() {
            bind(Boolean::class.java).annotatedWith(Names.named("EggIdMemoryEnabledProvider")).toInstance(false)
            bind(CryptoProvider::class.java).toInstance(mockk<CryptoProvider>())
        }
    }
}
