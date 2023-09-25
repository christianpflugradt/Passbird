package de.pflugradts.passbird.application.boot.main

import com.google.inject.Inject
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort

internal class PassbirdTestMain @Inject constructor(
    @Inject val bootable: Bootable,
    @Inject val clipboardAdapterPort: ClipboardAdapterPort,
    @Inject val eventRegistry: EventRegistry,
    @Inject val importExportService: ImportExportService,
    @Inject val keyStoreAdapterPort: KeyStoreAdapterPort,
    @Inject val passwordProvider: PasswordProvider,
    @Inject val passwordService: PasswordService,
    @Inject val passwordStoreAdapterPort: PasswordStoreAdapterPort,
    @Inject val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject val configuration: ReadableConfiguration,
    @Inject val commandHandlers: Set<CommandHandler>,
    @Inject val eventHandlers: Set<EventHandler>,
)
