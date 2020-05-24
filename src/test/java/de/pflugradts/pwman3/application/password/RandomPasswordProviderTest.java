package de.pflugradts.pwman3.application.password;

import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.domain.service.RandomPasswordProvider;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RandomPasswordProviderTest {

    @Mock
    private Configuration configuration;
    @InjectMocks
    private RandomPasswordProvider passwordProvider;

    @BeforeEach
    private void setup() {
        ConfigurationFaker.faker().forInstance(configuration)
                .withPasswordLength(20).fake();
    }

    @Test
    void shouldTakeLengthFromConfiguration() {
        // given
        final var passwordLength = 25;
        ConfigurationFaker.faker().forInstance(configuration)
                .withPasswordLength(passwordLength).fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword().toByteArray())
                        .hasSize(passwordLength));
    }

    @Test
    void shouldIncludeDigits() {
        // given / when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword().asString()
                        .matches(".*[0-9].*")).isTrue());
    }

    @Test
    void shouldIncludeUppercase() {
        // given / when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword().asString()
                        .matches(".*[A-Z].*")).isTrue());
    }

    @Test
    void shouldIncludeLowercase() {
        // given / when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword().asString()
                        .matches(".*[a-z].*")).isTrue());
    }

    @Test
    void shouldIncludeSpecialCharacters_IfEnabled() {
        // given
        ConfigurationFaker.faker().forInstance(configuration)
                .withSpecialCharactersEnabled().fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword().asString()
                        .matches("^[0-9A-Za-z]+$")).isFalse());
    }

    @Test
    void shouldNotIncludeSpecialCharacters_IfDisabled() {
        // given
        ConfigurationFaker.faker().forInstance(configuration)
                .withSpecialCharactersDisabled().fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword().asString()
                        .matches("^[0-9A-Za-z]+$")).isTrue());
    }

    private void assertManyTimes(final Supplier supplier) {
        IntStream.range(0, 10).forEach(i -> supplier.get());
    }

}
