package de.pflugradts.pwman3.application.boot.main;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.ClipboardAdapterPort;
import de.pflugradts.pwman3.application.KeyStoreAdapterPort;
import de.pflugradts.pwman3.application.PasswordStoreAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.commandhandling.handler.CommandHandler;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.exchange.ImportExportService;
import de.pflugradts.pwman3.domain.service.PasswordProvider;
import de.pflugradts.pwman3.domain.service.PasswordService;
import java.util.Set;
import lombok.Getter;

@Getter
class PwMan3TestMain {

    @Inject
    private Bootable bootable;
    @Inject
    private ClipboardAdapterPort clipboardAdapterPort;
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

}
