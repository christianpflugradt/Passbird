package de.pflugradts.passbird.application.boot.main

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.util.Modules
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.commandhandling.handler.ChangeMasterPasswordCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.SetInfoCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.CustomSetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.DiscardCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.GetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.OneTimeSetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.RenameCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.SetCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.egg.ViewCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.favorite.AddFavoriteCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.favorite.DiscardFavoriteCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.favorite.FavoriteInfoCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.favorite.GetFavoriteCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.favorite.UseFavoriteCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.favorite.ViewFavoriteCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.memory.GetMemoryCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.memory.MemoryInfoCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.memory.UseMemoryCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.memory.ViewMemoryCommandHandler
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
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.domain.model.slot.Slot
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
        val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
        val actual = Guice.createInjector(Modules.override(ApplicationModule(runContext)).with(PassbirdTestModule()))
            .getInstance(PassbirdTestMain::class.java)

        // then
        expectThat(actual.bootable).isA<PassbirdApplication>()
        expectThat(actual.runContext) isSameInstanceAs runContext
        actual.commandHandlers.forEachIndexed { index, commandHandler ->
            expectThat(commandHandler::class.java) isSameInstanceAs expectedCommandHandlers[index]
        }
        actual.eventHandlers.forEachIndexed { index, eventHandler ->
            expectThat(eventHandler::class.java) isSameInstanceAs expectedEventHandlers[index]
        }
        actual.initializers.forEachIndexed { index, initializer ->
            expectThat(initializer::class.java) isSameInstanceAs expectedInitializers[index]
        }
        actual.finalizers.forEachIndexed { index, finalizer ->
            expectThat(finalizer::class.java) isSameInstanceAs expectedFinalizers[index]
        }
    }

    class PassbirdTestModule : AbstractModule() {
        public override fun configure() {
            bind(CryptoProvider::class.java).toInstance(mockk<CryptoProvider>())
        }
    }

    companion object {
        private val expectedCommandHandlers = listOf(
            AddFavoriteCommandHandler::class.java,
            AddNestCommandHandler::class.java,
            MoveToNestCommandHandler::class.java,
            CustomSetCommandHandler::class.java,
            OneTimeSetCommandHandler::class.java,
            ChangeMasterPasswordCommandHandler::class.java,
            DiscardCommandHandler::class.java,
            DiscardFavoriteCommandHandler::class.java,
            DiscardNestCommandHandler::class.java,
            DiscardProteinCommandHandler::class.java,
            ExportCommandHandler::class.java,
            FavoriteInfoCommandHandler::class.java,
            GetFavoriteCommandHandler::class.java,
            GetCommandHandler::class.java,
            GetMemoryCommandHandler::class.java,
            GetProteinCommandHandler::class.java,
            HelpCommandHandler::class.java,
            ImportCommandHandler::class.java,
            ListCommandHandler::class.java,
            MemoryInfoCommandHandler::class.java,
            ProteinInfoCommandHandler::class.java,
            QuitCommandHandler::class.java,
            RenameCommandHandler::class.java,
            SetCommandHandler::class.java,
            SetInfoCommandHandler::class.java,
            SetProteinCommandHandler::class.java,
            SwitchNestCommandHandler::class.java,
            UseFavoriteCommandHandler::class.java,
            UseMemoryCommandHandler::class.java,
            ViewCommandHandler::class.java,
            ViewFavoriteCommandHandler::class.java,
            ViewMemoryCommandHandler::class.java,
            ViewNestCommandHandler::class.java,
            ViewProteinStructuresCommandHandler::class.java,
            ViewProteinTypesCommandHandler::class.java,
        )
        private val expectedEventHandlers = listOf(
            ApplicationEventHandler::class.java,
            DomainEventHandler::class.java,
        )
        private val expectedInitializers = listOf(
            ExportFileChecker::class.java,
            InactivityHandlerScheduler::class.java,
        )
        private val expectedFinalizers = listOf(
            BackupManager::class.java,
        )
    }
}
