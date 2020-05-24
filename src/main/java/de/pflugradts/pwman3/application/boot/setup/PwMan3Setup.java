package de.pflugradts.pwman3.application.boot.setup;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.KeyStoreAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.configuration.ConfigurationSync;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.KEYSTORE_FILENAME;

public class PwMan3Setup implements Bootable {

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
        return userInterfaceAdapterPort.receive()
                .onFailure(failureCollector::acceptInputFailure)
                .getOrElse(Input.empty())
                .getCommandChar() == 'c';
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
                    .onFailure(failureCollector::acceptInputFailure)
                    .getOrElse(Input.empty());
            inputRepeated = userInterfaceAdapterPort.receiveSecurely(Output.of(Bytes.of("second input: ")))
                    .onFailure(failureCollector::acceptInputFailure)
                    .getOrElse(Input.empty());
        }
        userInterfaceAdapterPort.sendLineBreak();
        return input;
    }

    private boolean inputEqualsAndIsNotEmpty(final Input input, final Input inputRepeated) {
        return Objects.nonNull(input)
                && Objects.equals(input, inputRepeated)
                && !Objects.equals(input, Input.empty());
    }

    private void createKeyStore(final String directory, final Input password) {
        keyStoreAdapterPort.storeKey(password.getBytes().toChars(), Paths.get(directory).resolve(KEYSTORE_FILENAME))
                .onSuccess(setupGuide::sendCreateKeyStoreSucceeded)
                .onFailure(setupGuide::sendCreateKeyStoreFailed);
    }

    private String verifyValidDirectory(final String source) {
        var directory = source;
        while (!isValidDirectory(directory)) {
            directory = userInterfaceAdapterPort.receive(Output.of(Bytes.of("your input: ")))
                    .onFailure(failureCollector::acceptInputFailure)
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
