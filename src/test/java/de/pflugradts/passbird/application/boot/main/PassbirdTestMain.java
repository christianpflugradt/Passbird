package de.pflugradts.passbird.application.boot.main;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.ClipboardAdapterPort;
import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.exchange.ImportExportService;
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort;
import java.util.Set;
import lombok.Getter;

@Getter
class PassbirdTestMain {

    @Inject
    private Bootable bootable;
    @Inject
    private ClipboardAdapterPort clipboardAdapterPort;
    @Inject
    private EventRegistry eventRegistry;
    @Inject
    private ImportExportService importExportService;
    @Inject
    private KeyStoreAdapterPort keyStoreAdapterPort;
    @Inject
    private PasswordProvider passwordProvider;
    @Inject
    private PasswordService passwordService;
    @Inject
    private PasswordStoreAdapterPort passwordStoreAdapterPort;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private Set<CommandHandler> commandHandlers;
    @Inject
    private Set<EventHandler> eventHandlers;

}
