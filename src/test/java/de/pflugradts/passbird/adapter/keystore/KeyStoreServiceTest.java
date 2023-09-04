package de.pflugradts.passbird.adapter.keystore;

import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.application.util.SystemOperationFaker;
import de.pflugradts.passbird.domain.model.transfer.Chars;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KeyStoreServiceTest {

    @Test
    void shouldStoreKey_FailOnInvalidPath() {
        // given / when
        final var actual = setupKeyStoreService().storeKey(Chars.charsOf("password".toCharArray()), mock(Path.class));

        // then
        assertThat(actual.isFailure()).isTrue();
    }

    @Test
    void shouldLoadKey_FailOnInvalidPath() {
        // given / when
        final var actual = setupKeyStoreService().loadKey(Chars.charsOf("password".toCharArray()), mock(Path.class));

        // then
        assertThat(actual.isFailure()).isTrue();
    }

    @Test
    void shouldStoreKey_FailOnKeyStoreUnavailable() {
        // given / when
        final var mockedSystemOperation = SystemOperationFaker.faker()
                .fakeSystemOperation()
                .withKeyStoreUnavailable().fake();
        final var actual = setupKeyStoreService(mockedSystemOperation)
                .storeKey(Chars.charsOf("password".toCharArray()), Paths.get(""));

        // then
        assertThat(actual.isFailure()).isTrue();
    }

    @Test
    void shouldLoadKey_FailOnKeyStoreUnavailable() {
        // given / when
        final var mockedSystemOperation = SystemOperationFaker.faker()
                .fakeSystemOperation()
                .withKeyStoreUnavailable().fake();
        final var actual = setupKeyStoreService(mockedSystemOperation)
                .loadKey(Chars.charsOf("password".toCharArray()), Paths.get(""));

        // then
        assertThat(actual.isFailure()).isTrue();
    }

    private KeyStoreService setupKeyStoreService() {
        return setupKeyStoreService(new SystemOperation());
    }

    private KeyStoreService setupKeyStoreService(final SystemOperation systemOperation) {
        return new KeyStoreService(systemOperation);
    }

}
