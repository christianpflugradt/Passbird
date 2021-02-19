package de.pflugradts.pwman3.application.failurehandling;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.failurehandling.failure.ChecksumFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.CommandFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.DecryptPasswordDatabaseFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ExportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.ImportFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.InputFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.PasswordEntriesFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.PasswordEntryFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.RenamePasswordEntryFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.SignatureCheckFailure;
import de.pflugradts.pwman3.application.failurehandling.failure.WritePasswordDatabaseFailure;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.password.InvalidKeyException;
import de.pflugradts.pwman3.domain.service.eventhandling.EventHandler;

import java.nio.file.Path;

@SuppressWarnings("PMD.TooManyMethods")
public class FailureHandler implements EventHandler {

    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private Bootable bootable;
    @Inject
    private SystemOperation systemOperation;

    @Subscribe
    private void handle(final ChecksumFailure checksumFailure) {
        err("Checksum of password database could not be verified.");
        if (configuration.getAdapter().getPasswordStore().isVerifyChecksum()) {
            err("Shutting down due to checksum failure. If you still think this is a valid PwMan3 password "
                    + "database file, you can set the verifyChecksum option in your configuration to false.");
            bootable.terminate(systemOperation);
        }
    }

    @Subscribe
    private void handle(final CommandFailure commandFailure) {
        err(commandFailure.getThrowable().getMessage());
    }

    @Subscribe
    private void handle(final RenamePasswordEntryFailure renamePasswordEntryFailure) {
        err("Password alias could not be renamed: %s", renamePasswordEntryFailure.getThrowable().getMessage());
    }

    @Subscribe
    private void handle(final PasswordEntryFailure passwordEntryFailure) {
        if (passwordEntryFailure.getThrowable() instanceof InvalidKeyException) {
            err("Password alias cannot contain digits or special characters. Please choose a different alias.");
        } else {
            err("Password entry could not be accessed.");
        }
    }

    @Subscribe
    private void handle(final PasswordEntriesFailure passwordEntriesFailure) {
        err("Password Entries could not be accessed.");
    }

    @Subscribe
    private void handle(final ExportFailure exportFailure) {
        err("Password database could not be exported");
    }

    @Subscribe
    private void handle(final ImportFailure importFailure) {
        if (importFailure.getThrowable() instanceof InvalidKeyException) {
            err("Password file could not be imported because "
                            + "at least one alias contains digits or special characters. "
                            + "Please correct invalid aliases and try again. Errorneous alias: %s",
                    ((InvalidKeyException) importFailure.getThrowable()).getKeyBytes().asString());
        } else {
            err("Password file could not be imported.");
        }
    }

    @Subscribe
    private void handle(final InputFailure inputFailure) {
        err("Input could not be processed.");
    }

    @Subscribe
    private void handle(final DecryptPasswordDatabaseFailure decryptPasswordDatabaseFailure) {
        err(String.format(
                "Password database could not be decrypted. Please delete the file %s and reboot PwMan3.",
                getPasswordStoreLocationFromConfiguration()));
        bootable.terminate(systemOperation);
    }

    @Subscribe
    private void handle(final SignatureCheckFailure signatureCheckFailure) {
        err("Signature of password database could not be verified.");
        if (configuration.getAdapter().getPasswordStore().isVerifySignature()) {
            err("Shutting down due to signature failure. If you still think this is a valid PwMan3 password "
                    + "database file, you can set the verifySignature option in your configuration to false.");
            bootable.terminate(systemOperation);
        }
    }

    @Subscribe
    private void handle(final WritePasswordDatabaseFailure writePasswordDatabaseFailure) {
        err("Password database could not be synced.");
    }

    private void err(final String template, final Object... params) {
        System.err.println(String.format(template, params));
    }

    private Path getPasswordStoreLocationFromConfiguration() {
        return systemOperation.resolvePath(
                configuration.getAdapter().getPasswordStore().getLocation(),
                Configuration.DATABASE_FILENAME
        ).getOrNull();
    }

}
