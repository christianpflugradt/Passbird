package de.pflugradts.passbird.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.util.SystemOperation;

import java.util.Optional;

import static de.pflugradts.passbird.application.configuration.ReadableConfiguration.CONFIGURATION_FILENAME;
import static de.pflugradts.passbird.application.configuration.ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY;

public class ConfigurationFactory {

    @Inject
    private SystemOperation systemOperation;

    public UpdatableConfiguration loadConfiguration() {
        return Optional.ofNullable(configurationFromFile()).orElse(Configuration.createTemplate());
    }

    private UpdatableConfiguration configurationFromFile() {
        final var directory = System.getProperty(CONFIGURATION_SYSTEM_PROPERTY);
        final var mapper = new ObjectMapper(new YAMLFactory());
        try {
        return mapper.readValue(
                systemOperation.getPath(directory).resolve(CONFIGURATION_FILENAME).toFile(),
                Configuration.class);
        } catch (Exception ex) {
            // FIXME error handling
            return null;
        }
    }

}
