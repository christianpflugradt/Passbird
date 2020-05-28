package de.pflugradts.pwman3.application.configuration;

import de.pflugradts.pwman3.application.util.SystemOperation;
import java.io.File;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReadableConfigurationTestIT {

    @Spy
    private SystemOperation systemOperation;
    @InjectMocks
    private ConfigurationFactory configurationFactory;

    private String tempConfigurationDirectory;
    private String configurationFile;

    @BeforeEach
    private void setup() {
        tempConfigurationDirectory = UUID.randomUUID().toString();
        configurationFile = tempConfigurationDirectory + File.separator + ReadableConfiguration.CONFIGURATION_FILENAME;
        assertThat(new File(tempConfigurationDirectory).mkdir()).isTrue();
    }

    @AfterEach
    private void cleanup() {
        assertThat(new File(configurationFile).delete()).isTrue();
        assertThat(new File(tempConfigurationDirectory).delete()).isTrue();
    }

    @Test
    void shouldUseConfigurationFile_Roundtrip() {
        // first load template if physical files does not exist
        System.setProperty(CONFIGURATION_SYSTEM_PROPERTY, tempConfigurationDirectory);
        final var configuration = configurationFactory.loadConfiguration();
        assertThat(configuration.isTemplate()).isTrue();

        // now persist configuration to file system
        new ConfigurationSyncService(configuration).sync(tempConfigurationDirectory);

        // now load the persisted configuration and ensure the given configuration directory has been persisted too
        final var loadedConfiguration = configurationFactory.loadConfiguration();
        assertThat(Optional.ofNullable(loadedConfiguration))
                .map(ReadableConfiguration::getAdapter)
                .map(ReadableConfiguration.Adapter::getKeyStore)
                .map(ReadableConfiguration.KeyStore::getLocation)
                .isPresent().get().asString()
                .isEqualTo(tempConfigurationDirectory);
        assertThat(loadedConfiguration).isNotNull()
                .extracting(ReadableConfiguration::isTemplate)
                .isEqualTo(false);
    }

}
