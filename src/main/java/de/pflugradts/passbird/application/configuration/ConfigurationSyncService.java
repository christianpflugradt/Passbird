package de.pflugradts.passbird.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.util.SystemOperation;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import static com.fasterxml.jackson.databind.MapperFeature.PROPAGATE_TRANSIENT_MARKER;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static de.pflugradts.passbird.application.configuration.ReadableConfiguration.CONFIGURATION_FILENAME;

@AllArgsConstructor
@NoArgsConstructor
public class ConfigurationSyncService implements ConfigurationSync {

    @Inject
    private UpdatableConfiguration updatableConfiguration;
    @Inject
    private SystemOperation systemOperation;

    @Override
    public Try<Void> sync(final String directory) {
        updatableConfiguration.updateDirectory(directory);
        return Try.run(() -> new ObjectMapper(new YAMLFactory()
                .disable(WRITE_DOC_START_MARKER))
                .enable(PROPAGATE_TRANSIENT_MARKER)
                .writeValue(
                        systemOperation.resolvePathToFile(directory, CONFIGURATION_FILENAME).getOrNull(),
                        updatableConfiguration)
        );
    }

}
