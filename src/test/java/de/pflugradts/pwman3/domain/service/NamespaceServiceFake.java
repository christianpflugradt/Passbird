package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.password.PasswordEntryRepositoryFaker;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;

import static org.mockito.Mockito.mock;

public class NamespaceServiceFake extends NamespaceService {

    public NamespaceServiceFake() {
        super(false, fakePasswordEntryRepository());
        populateEmpty();
    }

    private static PasswordEntryRepository fakePasswordEntryRepository() {
        return PasswordEntryRepositoryFaker.faker().forInstance(mock(PasswordEntryRepository.class)).fake();
    }

}
