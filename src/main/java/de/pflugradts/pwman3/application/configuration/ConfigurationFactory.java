package de.pflugradts.pwman3.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vavr.control.Try;

import java.nio.file.Paths;

import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_FILENAME;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY;

public class ConfigurationFactory {

    private static final int DEFAULT_DELAY_SECONDS = 10;
    private static final int DEFAULT_PASSWORD_LENGTH = 20;

    public UpdatableConfiguration loadConfiguration() {
        return configurationFromFile().getOrElse(this::defaultConfiguration);
    }

    private Try<UpdatableConfiguration> configurationFromFile() {
        final var directory = System.getProperty(CONFIGURATION_SYSTEM_PROPERTY);
        final var mapper = new ObjectMapper(new YAMLFactory());
        return Try.of(() -> mapper.readValue(
                Paths.get(directory).resolve(CONFIGURATION_FILENAME).toFile(),
                Configuration.class)
        );
    }

    private UpdatableConfiguration defaultConfiguration() {
        return Configuration.builder()
                .template(true)
                .adapter(Configuration.Adapter.builder()
                        .clipboard(Configuration.Clipboard.builder()
                                .reset(Configuration.ClipboardReset.builder()
                                        .delaySeconds(DEFAULT_DELAY_SECONDS)
                                        .enabled(true)
                                        .build())
                                .build())
                        .keyStore(Configuration.KeyStore.builder()
                                .location("")
                                .build())
                        .passwordStore(Configuration.PasswordStore.builder()
                                .location("")
                                .verifySignature(true)
                                .verifyChecksum(true)
                                .build())
                        .userInterface(Configuration.UserInterface.builder()
                                .secureInput(true)
                                .build())
                        .build())
                .application(Configuration.Application.builder()
                        .password(Configuration.Password.builder()
                                .length(DEFAULT_PASSWORD_LENGTH)
                                .specialCharacters(true)
                                .promptOnRemoval(true)
                                .build())
                        .build())
                .build();
    }

}
