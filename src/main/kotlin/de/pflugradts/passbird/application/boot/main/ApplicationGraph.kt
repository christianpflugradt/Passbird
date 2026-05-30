package de.pflugradts.passbird.application.boot.main

import de.pflugradts.passbird.adapter.clipboard.ClipboardGateway
import de.pflugradts.passbird.adapter.clipboard.ClipboardService
import de.pflugradts.passbird.adapter.exchange.FilePasswordExchange
import de.pflugradts.passbird.adapter.keystore.KeyStoreFactory
import de.pflugradts.passbird.adapter.keystore.KeyStoreService
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeFacade
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeReader
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeWriter
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.adapter.userinterface.TerminalInputGateway
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.CommandBus
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.CommandHandlerBus
import de.pflugradts.passbird.application.commandhandling.CommandInputHandler
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.RememberedCommandMemory
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.factory.CommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.FavoriteCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.ListCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.MemoryCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.NestCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.ProteinCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.SetCommandFactory
import de.pflugradts.passbird.application.commandhandling.handler.ChangeMasterPasswordCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.RepeatLastCommandHandler
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
import de.pflugradts.passbird.application.configuration.ConfigurationFactory
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.eventhandling.ApplicationEventHandler
import de.pflugradts.passbird.application.eventhandling.PassbirdEventRegistry
import de.pflugradts.passbird.application.exchange.ExchangeFactory
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.exchange.PasswordImportExportService
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.application.process.backup.BackupManager
import de.pflugradts.passbird.application.process.exchange.ExportFileChecker
import de.pflugradts.passbird.application.process.inactivity.InactivityHandler
import de.pflugradts.passbird.application.process.inactivity.InactivityHandlerScheduler
import de.pflugradts.passbird.application.process.inactivity.InactivityTerminationSignal
import de.pflugradts.passbird.application.security.CryptoProviderFactory
import de.pflugradts.passbird.application.security.KeyStoreAuthenticationService
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.nest.NestStateView
import de.pflugradts.passbird.domain.service.nest.NestingGroundService
import de.pflugradts.passbird.domain.service.password.DiscardPasswordService
import de.pflugradts.passbird.domain.service.password.FavoritePasswordService
import de.pflugradts.passbird.domain.service.password.MovePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordFacade
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PutPasswordService
import de.pflugradts.passbird.domain.service.password.RenamePasswordService
import de.pflugradts.passbird.domain.service.password.ViewPasswordService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.provider.RandomPasswordProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import de.pflugradts.passbird.domain.service.password.tree.NestingGround
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeSyncService

class ApplicationGraph(
    val runContext: RunContext,
    private val cryptoProviderOverride: CryptoProvider? = null,
) {
    val bootable: Bootable get() = passbirdApplication
    val clipboardAdapterPort: ClipboardAdapterPort by lazy { ClipboardService(ClipboardGateway(), configuration) }
    val commandHandlers: Set<CommandHandler> by lazy {
        setOf(
            AddFavoriteCommandHandler(passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            AddNestCommandHandler(nestService, userInterfaceAdapterPort, commandExecutionTracker),
            MoveToNestCommandHandler(
                canListAvailableNests,
                nestService,
                passwordService,
                userInterfaceAdapterPort,
                commandExecutionTracker,
            ),
            CustomSetCommandHandler(configuration, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            OneTimeSetCommandHandler(
                configuration,
                passwordService,
                passwordProvider,
                userInterfaceAdapterPort,
                commandExecutionTracker,
            ),
            ChangeMasterPasswordCommandHandler(
                keyStoreAdapterPort,
                keyStoreAuthenticationService,
                userInterfaceAdapterPort,
                commandExecutionTracker,
            ),
            DiscardCommandHandler(configuration, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            DiscardFavoriteCommandHandler(passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            DiscardNestCommandHandler(nestService, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            DiscardProteinCommandHandler(configuration, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            ExportCommandHandler(
                canListAvailableNests,
                importExportService,
                nestService,
                userInterfaceAdapterPort,
                commandExecutionTracker,
            ),
            FavoriteInfoCommandHandler(canPrintInfo, userInterfaceAdapterPort),
            GetFavoriteCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort, commandExecutionTracker),
            GetCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort, commandExecutionTracker),
            GetMemoryCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort, commandExecutionTracker),
            GetProteinCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort, commandExecutionTracker),
            HelpCommandHandler(canPrintInfo, userInterfaceAdapterPort),
            ImportCommandHandler(
                configuration,
                importExportService,
                nestService,
                passwordService,
                userInterfaceAdapterPort,
                commandExecutionTracker,
            ),
            ListCommandHandler(nestService, passwordService, userInterfaceAdapterPort),
            MemoryInfoCommandHandler(canPrintInfo, userInterfaceAdapterPort),
            ProteinInfoCommandHandler(canPrintInfo, userInterfaceAdapterPort),
            QuitCommandHandler(finalizers, userInterfaceAdapterPort, systemOperation),
            RepeatLastCommandHandler({ inputHandler }, rememberedCommandMemory, userInterfaceAdapterPort, commandExecutionTracker),
            RenameCommandHandler(passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            SetCommandHandler(
                configuration,
                passwordService,
                passwordProvider,
                userInterfaceAdapterPort,
                commandExecutionTracker,
            ),
            SetInfoCommandHandler(canPrintInfo, configuration, userInterfaceAdapterPort),
            SetProteinCommandHandler(configuration, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            SwitchNestCommandHandler(nestService, userInterfaceAdapterPort, commandExecutionTracker),
            UseFavoriteCommandHandler({ inputHandler }, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            UseMemoryCommandHandler({ inputHandler }, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            ViewCommandHandler(passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            ViewFavoriteCommandHandler(canPrintInfo, passwordService, userInterfaceAdapterPort),
            ViewMemoryCommandHandler(canPrintInfo, passwordService, userInterfaceAdapterPort),
            ViewNestCommandHandler(canPrintInfo, canListAvailableNests, nestService, userInterfaceAdapterPort),
            ViewProteinStructuresCommandHandler(canPrintInfo, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
            ViewProteinTypesCommandHandler(canPrintInfo, passwordService, userInterfaceAdapterPort, commandExecutionTracker),
        )
    }
    val configuration: ReadableConfiguration by lazy { configurationFactory.loadConfiguration() }
    val eventHandlers: Set<EventHandler> by lazy {
        setOf(
            ApplicationEventHandler(cryptoProvider, userInterfaceAdapterPort),
            DomainEventHandler { eggRepository },
        )
    }
    val eventRegistry: EventRegistry by lazy { PassbirdEventRegistry(eventHandlers) }
    val finalizers: Set<Finalizer> by lazy { setOf(backupManager) }
    val importExportService: ImportExportService by lazy {
        PasswordImportExportService(exchangeFactory, passwordService, nestService, eventRegistry)
    }
    val initializers: Set<Initializer> by lazy { setOf(exportFileChecker, inactivityHandlerScheduler) }
    val inputHandler: InputHandler by lazy {
        CommandInputHandler(commandBus, commandFactory, rememberedCommandMemory, commandExecutionTracker)
    }
    val keyStoreAdapterPort: KeyStoreAdapterPort by lazy { KeyStoreService(systemOperation, KeyStoreFactory()) }
    val nestService: NestService get() = nestingGroundService
    val nestStateView: NestStateView get() = nestingGroundService
    val passwordProvider: PasswordProvider by lazy { RandomPasswordProvider() }
    val passwordService: PasswordService by lazy {
        PasswordFacade(
            favoritePasswordService = favoritePasswordService,
            putPasswordService = putPasswordService,
            viewPasswordService = viewPasswordService,
            discardPasswordService = discardPasswordService,
            renamePasswordService = renamePasswordService,
            movePasswordService = movePasswordService,
        )
    }
    val passwordTreeAdapterPort: PasswordTreeAdapterPort by lazy { PasswordTreeFacade(passwordTreeReader, passwordTreeWriter) }
    val userInterfaceAdapterPort: UserInterfaceAdapterPort by lazy {
        CommandLineInterfaceService(TerminalInputGateway(), configuration, inactivityTerminationSignal)
    }

    private val systemOperation by lazy { SystemOperation() }
    private val configurationFactory by lazy { ConfigurationFactory(systemOperation, runContext) }
    private val cryptoProvider: CryptoProvider by lazy {
        cryptoProviderOverride ?: CryptoProviderFactory(keyStoreAuthenticationService, systemOperation).createCryptoProvider()
    }
    private val commandBus: CommandBus by lazy { CommandHandlerBus(commandHandlers) }
    private val commandExecutionTracker by lazy { CommandExecutionTracker() }
    private val commandFactory by lazy {
        CommandFactory(
            favoriteCommandFactory = FavoriteCommandFactory(),
            listCommandFactory = ListCommandFactory(),
            memoryCommandFactory = MemoryCommandFactory(),
            nestCommandFactory = NestCommandFactory(),
            proteinCommandFactory = ProteinCommandFactory(),
            setCommandFactory = SetCommandFactory(),
        )
    }
    private val rememberedCommandMemory by lazy { RememberedCommandMemory() }
    private val exchangeFactory by lazy { ExchangeFactory { FilePasswordExchange(systemOperation, runContext) } }
    private val keyStoreAuthenticationService by lazy {
        KeyStoreAuthenticationService(configuration, keyStoreAdapterPort, userInterfaceAdapterPort, systemOperation)
    }
    private val passwordTreeEnvelope by lazy { PasswordTreeEnvelope() }
    private val passwordTreePayloadReader by lazy { PasswordTreePayloadReader(configuration, systemOperation) }
    private val passwordTreePayloadWriter by lazy { PasswordTreePayloadWriter() }
    private val passwordTreeReader by lazy {
        PasswordTreeReader(systemOperation, configuration, cryptoProvider, passwordTreeEnvelope, passwordTreePayloadReader)
    }
    private val passwordTreeWriter by lazy {
        PasswordTreeWriter(systemOperation, configuration, cryptoProvider, passwordTreeEnvelope, passwordTreePayloadWriter)
    }
    private val eggRepository: EggRepository by lazy {
        NestingGround(
            eggIdMemoryEnabled = configuration.domain.eggIdMemory.enabled,
            passwordTreeAdapterPort = passwordTreeAdapterPort,
            nestStateView = nestStateView,
            eventRegistry = eventRegistry,
        )
    }
    private val passwordTreeSyncService by lazy { PasswordTreeSyncService { eggRepository } }
    private val nestingGroundService by lazy {
        NestingGroundService(passwordTreeAdapterPort, passwordTreeSyncService, eventRegistry)
    }
    private val favoritePasswordService by lazy {
        FavoritePasswordService(cryptoProvider, eggRepository, eventRegistry)
    }
    private val putPasswordService by lazy {
        PutPasswordService(cryptoProvider, eggRepository, eventRegistry, nestService)
    }
    private val viewPasswordService by lazy {
        ViewPasswordService(cryptoProvider, eggRepository, eventRegistry)
    }
    private val discardPasswordService by lazy {
        DiscardPasswordService(cryptoProvider, eggRepository, eventRegistry)
    }
    private val renamePasswordService by lazy {
        RenamePasswordService(cryptoProvider, eggRepository, eventRegistry)
    }
    private val movePasswordService by lazy {
        MovePasswordService(cryptoProvider, eggRepository, eventRegistry)
    }
    private val canPrintInfo by lazy { CanPrintInfo() }
    private val canListAvailableNests by lazy { CanListAvailableNests(nestService) }
    private val backupManager by lazy {
        BackupManager(configuration, runContext, systemOperation, cryptoProvider, passwordTreeEnvelope, passwordTreeAdapterPort)
    }
    private val exportFileChecker by lazy {
        ExportFileChecker(configuration, runContext, systemOperation, userInterfaceAdapterPort)
    }
    private val inactivityTerminationSignal by lazy { InactivityTerminationSignal() }
    private val inactivityHandler by lazy {
        InactivityHandler(commandBus, configuration, inactivityTerminationSignal, systemOperation)
    }
    private val inactivityHandlerScheduler by lazy { InactivityHandlerScheduler(configuration, inactivityHandler) }
    private val passbirdApplication by lazy {
        PassbirdApplication(
            inactivityHandler = inactivityHandler,
            initializers = initializers,
            inputHandler = inputHandler,
            nestService = nestService,
            runContext = runContext,
            userInterfaceAdapterPort = userInterfaceAdapterPort,
        )
    }
}
