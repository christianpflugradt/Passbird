package de.pflugradts.pwman3.application.password;

import de.pflugradts.pwman3.domain.model.password.PasswordRequirementsFaker;
import de.pflugradts.pwman3.domain.service.password.provider.RandomPasswordProvider;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RandomPasswordProviderTest {

    @InjectMocks
    private RandomPasswordProvider passwordProvider;

    @Test
    void shouldUseGivenLength() {
        // given
        final var passwordLength = 25;
        final var passwordRequirements = PasswordRequirementsFaker.faker()
                .fakePasswordRequirements()
                .withPasswordLength(passwordLength).fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword(passwordRequirements).toByteArray())
                        .hasSize(passwordLength));
    }

    @Test
    void shouldIncludeDigits() {
        // given
        final var passwordRequirements = PasswordRequirementsFaker.faker()
                .fakePasswordRequirements().fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword(passwordRequirements).asString()
                        .matches(".*[0-9].*")).isTrue());
    }

    @Test
    void shouldIncludeUppercase() {
        // given
        final var passwordRequirements = PasswordRequirementsFaker.faker()
                .fakePasswordRequirements().fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword(passwordRequirements).asString()
                        .matches(".*[A-Z].*")).isTrue());
    }

    @Test
    void shouldIncludeLowercase() {
        // given
        final var passwordRequirements = PasswordRequirementsFaker.faker()
                .fakePasswordRequirements().fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword(passwordRequirements).asString()
                        .matches(".*[a-z].*")).isTrue());
    }

    @Test
    void shouldIncludeSpecialCharacters_IfEnabled() {
        // given
        final var passwordRequirements = PasswordRequirementsFaker.faker()
                .fakePasswordRequirements()
                .withUseSpecialCharactersEnabled().fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword(passwordRequirements).asString()
                        .matches("^[0-9A-Za-z]+$")).isFalse());
    }

    @Test
    void shouldNotIncludeSpecialCharacters_IfDisabled() {
        // given
        final var passwordRequirements = PasswordRequirementsFaker.faker()
                .fakePasswordRequirements()
                .withUseSpecialCharactersDisabled().fake();

        // when / then
        assertManyTimes(() ->
                assertThat(passwordProvider.createNewPassword(passwordRequirements).asString()
                        .matches("^[0-9A-Za-z]+$")).isTrue());
    }

    private void assertManyTimes(final Supplier supplier) {
        IntStream.range(0, 10).forEach(i -> supplier.get());
    }

}
