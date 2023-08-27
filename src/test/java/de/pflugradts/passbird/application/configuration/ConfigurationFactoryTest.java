package de.pflugradts.passbird.application.configuration;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.util.SystemOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConfigurationFactoryTest {

    @Inject
    private SystemOperation systemOperation;
    @InjectMocks
    private ConfigurationFactory configurationFactory;

    @Test
    void shouldReturnDefaultConfiguration_WhenConfigurationFileDoesNotExist() {
        // given / when
        final var actual = configurationFactory.loadConfiguration();

        // then
        assertThat(actual).isNotNull().extracting(ReadableConfiguration::isTemplate).isEqualTo(true);
        assertThat(actual.getApplication()).isNotNull()
                .extracting(ReadableConfiguration.Application::getPassword).isNotNull()
                .extracting(ReadableConfiguration.Password::getLength)
                .isEqualTo(20);
    }

}
