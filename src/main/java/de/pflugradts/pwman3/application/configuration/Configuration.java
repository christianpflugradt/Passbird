package de.pflugradts.pwman3.application.configuration;

import com.google.inject.Singleton;
import de.pflugradts.pwman3.domain.model.password.PasswordRequirements;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Singleton
public class Configuration implements UpdatableConfiguration {

    private static final int DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS = 10;
    private static final int DEFAULT_PASSWORD_LENGTH = 20;

    private transient boolean template;
    private Application application = new Application();
    private Adapter adapter = new Adapter();

    static Configuration createTemplate() {
        final var configuration = new Configuration();
        configuration.template = true;
        return configuration;
    }

    @Override
    public PasswordRequirements parsePasswordRequirements() {
        return PasswordRequirements.of(
                getApplication().getPassword().isSpecialCharacters(),
                getApplication().getPassword().getLength()
        );
    }

    @Override
    public void updateDirectory(final String directory) {
        adapter.keyStore.location = directory;
        adapter.passwordStore.location = directory;
    }

    @Data
    @NoArgsConstructor
    public static class Application implements ReadableConfiguration.Application {
        private boolean verifyLicenseFilesExist = true;
        private Password password = new Password();
    }

    @Data
    @NoArgsConstructor
    public static class Password implements ReadableConfiguration.Password {
        private int length = DEFAULT_PASSWORD_LENGTH;
        private boolean specialCharacters = true;
        private boolean promptOnRemoval = true;
    }

    @Data
    @NoArgsConstructor
    public static class Adapter implements ReadableConfiguration.Adapter {
        private Clipboard clipboard = new Clipboard();
        private KeyStore keyStore = new KeyStore();
        private PasswordStore passwordStore = new PasswordStore();
        private UserInterface userInterface = new UserInterface();
    }

    @Data
    @NoArgsConstructor
    public static class Clipboard implements ReadableConfiguration.Clipboard {
        private ClipboardReset reset = new ClipboardReset();
    }

    @Data
    @NoArgsConstructor
    public static class ClipboardReset implements ReadableConfiguration.ClipboardReset {
        private boolean enabled = true;
        private int delaySeconds = DEFAULT_CLIPBOARD_RESET_DELAY_SECONDS;
    }

    @Data
    @NoArgsConstructor
    public static class PasswordStore implements ReadableConfiguration.PasswordStore {
        private String location = "";
        private boolean verifySignature = true;
        private boolean verifyChecksum = true;
    }

    @Data
    @NoArgsConstructor
    public static class KeyStore implements ReadableConfiguration.KeyStore {
        private String location = "";
    }

    @Data
    @NoArgsConstructor
    public static class UserInterface implements ReadableConfiguration.UserInterface {
        private boolean secureInput = true;
    }

}
