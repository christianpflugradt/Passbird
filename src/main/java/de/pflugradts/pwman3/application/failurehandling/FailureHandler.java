package de.pflugradts.pwman3.application.failurehandling;

import com.google.common.eventbus.Subscribe;
import de.pflugradts.pwman3.EventHandler;
import de.pflugradts.pwman3.application.failurehandling.failure.ChecksumFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.DecryptionFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.EncryptionFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ExportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ImportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.InputFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ReadPasswordDatabaseFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.SignatureCheckFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.WritePasswordDatabaseFailure;

public class FailureHandler implements EventHandler {

    @Subscribe
    private void handle(final ChecksumFailure checksumFailure) {
        err("Checksum of password database could not be verified.");
    }

    @Subscribe
    private void handle(final DecryptionFailure decryptionFailure) {
        err("Bytes could not be decrypted.");
    }

    @Subscribe
    private void handle(final EncryptionFailure encryptionFailure) {
        err("Bytes could not be encrypted.");
    }

    @Subscribe
    private void handle(final ExportFailure exportFailure) {
        err("Password database could not be exported");
    }

    @Subscribe
    private void handle(final ImportFailure importFailure) {
        err("Password file could not be imported.");
    }

    @Subscribe
    private void handle(final InputFailure inputFailure) {
        err("Input could not be processed.");
    }

    @Subscribe
    private void handle(final ReadPasswordDatabaseFailure readPasswordDatabaseFailure) {
        err("Password database could not be read.");
    }

    @Subscribe
    private void handle(final SignatureCheckFailure signatureCheckFailure) {
        err("Signature of password database could not be verified.");
    }

    @Subscribe
    private void handle(final WritePasswordDatabaseFailure writePasswordDatabaseFailure) {
        err("Password database could not be synced.");
    }

    private void err(final String message) {
        System.err.println(message);
    }

}
