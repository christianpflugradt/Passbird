package de.pflugradts.pwman3.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.util.SystemOperation;
import io.vavr.control.Try;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_FILENAME;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY;

public class ConfigurationFactory {

    @Inject
    private SystemOperation systemOperation;

    public UpdatableConfiguration loadConfiguration() {
        return configurationFromFile().getOrElse(Configuration::createTemplate);
    }

    private Try<UpdatableConfiguration> configurationFromFile() {
        final var directory = System.getProperty(CONFIGURATION_SYSTEM_PROPERTY);
        final var mapper = new ObjectMapper(new YAMLFactory());
        return Try.of(() -> mapper.readValue(
                systemOperation.getPath(directory).getOrNull().resolve(CONFIGURATION_FILENAME).toFile(),
                Configuration.class)
        );
    }

}
