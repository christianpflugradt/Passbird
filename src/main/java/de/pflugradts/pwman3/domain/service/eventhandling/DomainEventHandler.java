package de.pflugradts.pwman3.domain.service.eventhandling;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;

@Singleton
public class DomainEventHandler implements EventHandler {

    @Inject
    private PasswordEntryRepository passwordEntryRepository;

    @Subscribe
    private void handlePasswordEntryDiscarded(final PasswordEntryDiscarded passwordEntryDiscarded) {
        passwordEntryRepository.delete(passwordEntryDiscarded.getPasswordEntry());
    }

}
