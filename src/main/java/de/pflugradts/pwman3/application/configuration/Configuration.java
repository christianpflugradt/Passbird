package de.pflugradts.pwman3.application.configuration;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@Singleton
public class Configuration implements UpdatableConfiguration {

    private transient boolean template;
    private Application application;
    private Adapter adapter;

    @Override
    public void updateDirectory(final String directory) {
        adapter.keyStore.location = directory;
        adapter.passwordStore.location = directory;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class Application implements ReadableConfiguration.Application {
        private Password password;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class Password implements ReadableConfiguration.Password {
        private int length;
        private boolean specialCharacters;
        private boolean promptOnRemoval;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class Adapter implements ReadableConfiguration.Adapter {
        private Clipboard clipboard;
        private KeyStore keyStore;
        private PasswordStore passwordStore;
        private UserInterface userInterface;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class Clipboard implements ReadableConfiguration.Clipboard {
        private ClipboardReset reset;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class ClipboardReset implements ReadableConfiguration.ClipboardReset {
        private boolean enabled;
        private int delaySeconds;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class PasswordStore implements ReadableConfiguration.PasswordStore {
        private String location;
        private boolean verifySignature;
        private boolean verifyChecksum;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class KeyStore implements ReadableConfiguration.KeyStore {
        private String location;
    }

    @AllArgsConstructor
    @Builder
    @Data
    @NoArgsConstructor
    public static class UserInterface implements ReadableConfiguration.UserInterface {
        private boolean secureInput;
    }

}
