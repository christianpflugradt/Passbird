package de.pflugradts.passbird.application.boot.setup;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@SuppressWarnings("PMD.TooManyMethods")
@AllArgsConstructor
@NoArgsConstructor
class SetupGuide {

    private static final String CONFIGURATION = "ReadableConfiguration";
    private static final String KEYSTORE = "Keystore";

    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    void sendWelcome() {
        send("Welcome to PwMan3 Setup!");
        userInterfaceAdapterPort.sendLineBreak();
    }

    void sendConfigTemplateRouteInformation() {
        send("You have landed here because you did not provide a configuration.");
        send("If you have a configuration file, then please start PwMan3 as follows:");
        send("java -jar PwMan3.jar absolute-path-to-directory-with-configuration-file");
        userInterfaceAdapterPort.sendLineBreak();
        send("Example for a Linux path: /etc/pwman3");
        send("Example for a Windows path: c:\\programs\\pwman3");
        send("The configuration file must be in the specified directory and it must be named PwMan3.yml.");
        userInterfaceAdapterPort.sendLineBreak();
        send("To (c)ontinue setup, press 'c'. To quit setup, press any other key");
    }

    void sendConfigKeyStoreRouteInformation(final String location) {
        send("You have landed here because the keystore specified in your configuration does not exist.");
        send("Your configuration specifies the keystore to be in the following directory: " + location);
        send("However in that directory there is no file PwMan3.ks");
    }

    void sendInputPath(final String fileDescription) {
        send("Please input an absolute path to a directory in which to create the " + fileDescription + " file.");
    }

    void sendCreateKeyStoreInformation() {
        userInterfaceAdapterPort.sendLineBreak();
        send("Your PwMan3 Keystore will be secured by a master password.");
        send("This master password gives access to all passwords stored in PwMan3.");
        send("If you lose this password, you will not be able to access any passwords stored in PwMan3.");
        send("Choose your master password wisely.");
        userInterfaceAdapterPort.sendLineBreak();
        send("You have to input your master password twice.");
        send("Your input will be hidden unless secureInput is disabled in your configuration.");
        send("If your inputs do not match, you will have to repeat the procedure.");
        userInterfaceAdapterPort.sendLineBreak();
    }

    void sendCreateConfigurationSucceeded(final Void none) {
        sendCreateFileSucceeded(CONFIGURATION);
    }

    void sendCreateConfigurationFailed(final Throwable throwable) {
        sendCreateFileFailed(CONFIGURATION, throwable);
    }

    void sendCreateKeyStoreSucceeded(final Void none) {
        sendCreateFileSucceeded(KEYSTORE);
    }

    void sendCreateKeyStoreFailed(final Throwable throwable) {
        sendCreateFileFailed(KEYSTORE, throwable);
    }

    private void sendCreateFileSucceeded(final String fileDescription) {
        send(fileDescription + " has been created successfully!");
    }

    private void sendCreateFileFailed(final String fileDescription, final Throwable throwable) {
        send(fileDescription + " could not be created. Reason: " + throwable);
    }

    void sendNonMatchingInputs() {
        send("Your inputs do not match, please repeat.");
    }

    void sendRestart() {
        send("Now restart PwMan3 to use it.");
    }

    void sendGoodbye() {
        send("Goodbye!");
    }

    private void send(final String message) {
        userInterfaceAdapterPort.send(Output.of(Bytes.of(message)));
    }

}
