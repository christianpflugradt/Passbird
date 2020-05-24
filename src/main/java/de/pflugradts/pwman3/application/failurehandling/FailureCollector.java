package de.pflugradts.pwman3.application.failurehandling;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.failurehandling.failure.ChecksumFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.DecryptionFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.EncryptionFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ExportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.Failure;
import de.pflugradts.pwman3.application.failurehandling.failure.ImportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.InputFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ReadPasswordDatabaseFailure;
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

    public void acceptChecksumFailure(final Byte actualChecksum, final Byte expectedChecksum) {
        accept(new ChecksumFailure(actualChecksum, expectedChecksum));
    }

    public void acceptDecryptionFailure(final Bytes bytes, final Throwable throwable) {
        accept(new DecryptionFailure(bytes, throwable));
    }

    public void acceptEncryptionFailure(final Bytes bytes, final Throwable throwable) {
        accept(new EncryptionFailure(bytes, throwable));
    }

    public void acceptExportFailure(final Throwable throwable) {
        accept(new ExportFailure(throwable));
    }

    public void acceptImportFailure(final Throwable throwable) {
        accept(new ImportFailure(throwable));
    }

    public void acceptInputFailure(final Throwable throwable) {
        accept(new InputFailure(throwable));
    }

    public void acceptReadPasswordDatabaseFailure(final Path path, final Throwable throwable) {
        accept(new ReadPasswordDatabaseFailure(path, throwable));
    }

    public void acceptSignatureCheckFailure(final Bytes actualSignature) {
        accept(new SignatureCheckFailure(actualSignature));
    }

    public void acceptWritePasswordDatabaseFailure(final Path path, final Throwable throwable) {
        accept(new WritePasswordDatabaseFailure(path, throwable));
    }

    private void accept(final Failure failure) {
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
