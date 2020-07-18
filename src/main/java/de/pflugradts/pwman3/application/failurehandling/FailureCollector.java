package de.pflugradts.pwman3.application.failurehandling;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.failurehandling.failure.ChecksumFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.DecryptPasswordDatabaseFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ExportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.Failure;
import de.pflugradts.pwman3.application.failurehandling.failure.ImportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.InputFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.PasswordEntriesFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.PasswordEntryFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.SignatureCheckFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.WritePasswordDatabaseFailure;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import java.nio.file.Path;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "PMD.TooManyMethods"})
@NoArgsConstructor
@AllArgsConstructor
public class FailureCollector {

    private EventBus eventBus;

    @Inject
    private FailureHandler failureHandler;

    public void collectChecksumFailure(final Byte actualChecksum, final Byte expectedChecksum) {
        collect(new ChecksumFailure(actualChecksum, expectedChecksum));
    }

    public void collectPasswordEntryFailure(final Bytes bytes, final Throwable throwable) {
        collect(new PasswordEntryFailure(bytes, throwable));
    }

    public void collectPasswordEntriesFailure(final Throwable throwable) {
        collect(new PasswordEntriesFailure(throwable));
    }

    public void collectExportFailure(final Throwable throwable) {
        collect(new ExportFailure(throwable));
    }

    public void collectImportFailure(final Throwable throwable) {
        collect(new ImportFailure(throwable));
    }

    public void collectInputFailure(final Throwable throwable) {
        collect(new InputFailure(throwable));
    }

    public void collectDecryptPasswordDatabaseFailure(final Path path, final Throwable throwable) {
        collect(new DecryptPasswordDatabaseFailure(path, throwable));
    }

    public void collectSignatureCheckFailure(final Bytes actualSignature) {
        collect(new SignatureCheckFailure(actualSignature));
    }

    public void collectWritePasswordDatabaseFailure(final Path path, final Throwable throwable) {
        collect(new WritePasswordDatabaseFailure(path, throwable));
    }

    private void collect(final Failure failure) {
        getEventBus().post(failure);
    }

    private void initializeEventBus() {
        eventBus = new EventBus();
        eventBus.register(failureHandler);
    }

    private EventBus getEventBus() {
        if (Objects.isNull(eventBus)) {
            initializeEventBus();
        }
        return eventBus;
    }

}
