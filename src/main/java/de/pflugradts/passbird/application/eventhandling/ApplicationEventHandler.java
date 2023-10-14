package de.pflugradts.passbird.application.eventhandling;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.domain.model.PasswordEntryCreated;
import de.pflugradts.passbird.domain.model.PasswordEntryDiscarded;
import de.pflugradts.passbird.domain.model.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.PasswordEntryRenamed;
import de.pflugradts.passbird.domain.model.PasswordEntryUpdated;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;

public class ApplicationEventHandler implements EventHandler {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handlePasswordEntryCreated(final PasswordEntryCreated passwordEntryCreated) {
        var key = cryptoProvider.decrypt(passwordEntryCreated.getPasswordEntry().viewKey());
        sendToUserInterface("PasswordEntry '%s' successfully created.", key);
    }

    @Subscribe
    private void handlePasswordEntryUpdated(final PasswordEntryUpdated passwordEntryUpdated) {
        var key = cryptoProvider.decrypt(passwordEntryUpdated.getPasswordEntry().viewKey());
        sendToUserInterface("PasswordEntry '%s' successfully updated.", key);
    }

    @Subscribe
    private void handlePasswordEntryRenamed(final PasswordEntryRenamed passwordEntryRenamed) {
        var key = cryptoProvider.decrypt(passwordEntryRenamed.getPasswordEntry().viewKey());
        sendToUserInterface("PasswordEntry '%s' successfully renamed.", key);
    }

    @Subscribe
    private void handlePasswordEntryDiscarded(final PasswordEntryDiscarded passwordEntryDiscarded) {
        var key = cryptoProvider.decrypt(passwordEntryDiscarded.getPasswordEntry().viewKey());
        sendToUserInterface("PasswordEntry '%s' successfully deleted.", key);
    }

    @Subscribe
    private void handlePasswordEntryNotFound(final PasswordEntryNotFound passwordEntryNotFound) {
        var key = cryptoProvider.decrypt(passwordEntryNotFound.getKeyBytes());
        sendToUserInterface("PasswordEntry '%s' not found.", key);
    }

    private void sendToUserInterface(final String template, final Bytes keyBytes) {
        userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(
            String.format(template, keyBytes.asString()))));
    }

}
