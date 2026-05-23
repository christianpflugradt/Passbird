package de.pflugradts.passbird.application.boot.main

import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import jakarta.inject.Inject

class PassbirdTestMain @Inject constructor(
    val bootable: Bootable,
    val clipboardAdapterPort: ClipboardAdapterPort,
    val eventRegistry: EventRegistry,
    val importExportService: ImportExportService,
    val keyStoreAdapterPort: KeyStoreAdapterPort,
    val passwordProvider: PasswordProvider,
    val passwordService: PasswordService,
    val passwordTreeAdapterPort: PasswordTreeAdapterPort,
    val runContext: RunContext,
    val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    val configuration: ReadableConfiguration,
    val commandHandlers: Set<CommandHandler>,
    val eventHandlers: Set<EventHandler>,
    val initializers: Set<Initializer>,
    val finalizers: Set<Finalizer>,
)
