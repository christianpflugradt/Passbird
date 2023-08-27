package de.pflugradts.passbird.application.boot.setup;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.KeyStoreAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.configuration.ConfigurationSync;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import static de.pflugradts.passbird.application.configuration.ReadableConfiguration.KEYSTORE_FILENAME;

public class PassbirdSetup implements Bootable {

    @Inject
    private SetupGuide setupGuide;
    @Inject
    private ConfigurationSync configurationSync;
    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private KeyStoreAdapterPort keyStoreAdapterPort;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private SystemOperation systemOperation;

    @Override
    public void boot() {
        setupGuide.sendWelcome();
        if (configuration.isTemplate()) {
            setupGuide.sendConfigTemplateRouteInformation();
            if (continueRoute()) {
                configTemplateRoute();
            }
        } else {
            setupGuide.sendConfigKeyStoreRouteInformation(configuration.getAdapter().getKeyStore().getLocation());
            if (continueRoute()) {
                configKeyStoreRoute();
            }
        }
        setupGuide.sendGoodbye();
        terminate(systemOperation);
    }

    private boolean continueRoute() {
        return userInterfaceAdapterPort.receiveConfirmation(Output.of(Bytes.of("Your input: ")));
    }

    private void configTemplateRoute() {
        setupGuide.sendInputPath("configuration");
        createConfiguration(verifyValidDirectory(configuration.getAdapter().getPasswordStore().getLocation()));
        setupGuide.sendCreateKeyStoreInformation();
        createKeyStore(configuration.getAdapter().getKeyStore().getLocation(), receiveMasterPassword());
        setupGuide.sendRestart();
    }

    private void configKeyStoreRoute() {
        setupGuide.sendInputPath("keystore");
        setupGuide.sendCreateKeyStoreInformation();
        createKeyStore(
                verifyValidDirectory(configuration.getAdapter().getKeyStore().getLocation()),
                receiveMasterPassword());
        setupGuide.sendRestart();
    }

    private void createConfiguration(final String directory) {
        configurationSync.sync(directory)
                .onSuccess(setupGuide::sendCreateConfigurationSucceeded)
                .onFailure(setupGuide::sendCreateConfigurationFailed);
    }

    private Input receiveMasterPassword() {
        Input input = null;
        Input inputRepeated = null;
        while (!inputEqualsAndIsNotEmpty(input, inputRepeated)) {
            if (Objects.nonNull(input)) {
                setupGuide.sendNonMatchingInputs();
            }
            input = userInterfaceAdapterPort.receiveSecurely(Output.of(Bytes.of("first input: ")))
                    .onFailure(failureCollector::collectInputFailure)
                    .getOrElse(Input.empty());
            inputRepeated = userInterfaceAdapterPort.receiveSecurely(Output.of(Bytes.of("second input: ")))
                    .onFailure(failureCollector::collectInputFailure)
                    .getOrElse(Input.empty());
        }
        userInterfaceAdapterPort.sendLineBreak();
        return input;
    }

    private boolean inputEqualsAndIsNotEmpty(final Input input, final Input inputRepeated) {
        return Objects.nonNull(input) && !input.isEmpty() && Objects.equals(input, inputRepeated);
    }

    private void createKeyStore(final String directory, final Input password) {
        keyStoreAdapterPort.storeKey(
                password.getBytes().toChars(),
                systemOperation.resolvePath(directory, KEYSTORE_FILENAME).getOrNull())
            .onSuccess(setupGuide::sendCreateKeyStoreSucceeded)
            .onFailure(setupGuide::sendCreateKeyStoreFailed);
    }

    private String verifyValidDirectory(final String source) {
        var directory = source;
        while (!isValidDirectory(directory)) {
            directory = userInterfaceAdapterPort.receive(Output.of(Bytes.of("your input: ")))
                    .onFailure(failureCollector::collectInputFailure)
                    .getOrElse(Input.empty()).getBytes().asString();
        }
        return directory;
    }

    private boolean isValidDirectory(final String directory) {
        return Optional.ofNullable(systemOperation.getPath(directory)
                .getOrNull())
                .map(Path::toFile)
                .filter(File::isDirectory)
                .map(File::getParentFile)
                .filter(File::isDirectory)
                .filter(File::exists)
                .isPresent();
    }

}
