package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryRepositoryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
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

    public void deployAt(final NamespaceSlot namespaceSlot) {
        deploy(Bytes.of("namespace"), namespaceSlot);
    }

    public void deployAtIndex(final int index) {
        deployAt(NamespaceSlot.at(index));
    }

}
