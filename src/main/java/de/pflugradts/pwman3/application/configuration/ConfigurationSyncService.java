package de.pflugradts.pwman3.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import io.vavr.control.Try;

import java.nio.file.Paths;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.databind.MapperFeature.PROPAGATE_TRANSIENT_MARKER;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_FILENAME;

@AllArgsConstructor
@NoArgsConstructor
public class ConfigurationSyncService implements ConfigurationSync {

    @Inject
    private UpdatableConfiguration updatableConfiguration;

    @Override
    public Try<Void> sync(final String directory) {
        updatableConfiguration.updateDirectory(directory);
        return Try.run(() -> new ObjectMapper(new YAMLFactory()
                .disable(WRITE_DOC_START_MARKER))
                .enable(PROPAGATE_TRANSIENT_MARKER)
                .writeValue(Paths.get(directory).resolve(CONFIGURATION_FILENAME).toFile(), updatableConfiguration)
        );
    }

}
