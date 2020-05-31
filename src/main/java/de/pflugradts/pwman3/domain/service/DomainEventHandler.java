package de.pflugradts.pwman3.domain.service;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.EventHandler;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.security.CryptoProvider;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;

@Singleton
public class DomainEventHandler implements EventHandler {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handlePasswordEntryCreated(final PasswordEntryCreated passwordEntryCreated) {
        cryptoProvider
                .decrypt(passwordEntryCreated
                        .getPasswordEntry()
                        .viewKey())
                .onSuccess(key -> sendToUserInterface(
                        "PasswordEntry '%s' successfully created.", key));
    }

    @Subscribe
    private void handlePasswordEntryUpdated(final PasswordEntryUpdated passwordEntryUpdated) {
        cryptoProvider
                .decrypt(passwordEntryUpdated
                        .getPasswordEntry()
                        .viewKey())
                .onSuccess(key -> sendToUserInterface(
                        "PasswordEntry '%s' successfully updated.", key));
    }

    @Subscribe
    private void handlePasswordEntryDiscarded(final PasswordEntryDiscarded passwordEntryDiscarded) {
        cryptoProvider
                .decrypt(passwordEntryDiscarded
                        .getPasswordEntry()
                        .viewKey())
                .onSuccess(key -> sendToUserInterface(
                        "PasswordEntry '%s' successfully deleted.", key));
        passwordEntryRepository.delete(passwordEntryDiscarded.getPasswordEntry());

    }

    @Subscribe
    private void handlePasswordEntryNotFound(final PasswordEntryNotFound passwordEntryNotFound) {
        cryptoProvider
                .decrypt(passwordEntryNotFound
                        .getKeyBytes())
                .onSuccess(key -> sendToUserInterface(
                        "PasswordEntry '%s' not found.", key));
    }

    private void sendToUserInterface(final String template, final Bytes keyBytes) {
        userInterfaceAdapterPort.send(Output.of(Bytes.of(
                String.format(template, keyBytes.asString()))));
    }

}
