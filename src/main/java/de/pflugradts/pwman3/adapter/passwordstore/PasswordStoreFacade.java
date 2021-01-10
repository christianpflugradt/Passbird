package de.pflugradts.pwman3.adapter.passwordstore;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordStoreAdapterPort;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;
import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class PasswordStoreFacade implements PasswordStoreAdapterPort {

    @Inject
    private PasswordStoreReader passwordStoreReader;
    @Inject
    private PasswordStoreWriter passwordStoreWriter;

    @Override
    public Supplier<Stream<PasswordEntry>> restore() {
        return passwordStoreReader.restore();
    }

    @Override
    public void sync(final Supplier<Stream<PasswordEntry>> passwordEntriesSupplier) {
        passwordStoreWriter.sync(passwordEntriesSupplier);
    }

}
