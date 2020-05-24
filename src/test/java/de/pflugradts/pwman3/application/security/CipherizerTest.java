package de.pflugradts.pwman3.application.security;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CipherizerTest {

    private Cipherizer cryptoProvider;

    @BeforeEach
    private void setup() {
        final var secretBytes = Bytes.of(
                new byte[]{97, -87, -65, -105, -48, -75, 65, -72, 67, -25, -88, -123, -28, 42, -61, 39});
        final var ivBytes = Bytes.of(
                new byte[]{-93, -95, 112, -58, -16, -60, 90, 0, 60, 33, 24, -111, -40, 39, 87, -66}
        );
        cryptoProvider = new Cipherizer(secretBytes, ivBytes);
    }

    @Test
    void shouldEncryptBytes() {
        // given
        final var givenDecryptedBytes = Bytes.of(
                new byte[]{91, 87, 99, 52, 97, 79, 120, 82, 35, 40, 59, 77, 61, 111, 111, 110, 67, 102, 89, 108});
        final var expectedEncryptedBytes = Bytes.of(
                new byte[]{-66, 46, -25, 56, -75, -7, -111, 127, -107, -85, 113, 115, -25, -49, 4, -78, -92, -14, -27,
                        -19, -107, 49, -42, 39, 19, -43, 123, -125, 62, 40, 127, -2, 119, 13, 98, 29, -93, -115, 21, 14,
                        -95, -85, 109, 88, -83, -88, 96, 2});

        // when
        final var actual = cryptoProvider.encrypt(givenDecryptedBytes);

        // then
        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isNotNull().isEqualTo(expectedEncryptedBytes);
    }

    @Test
    void shouldDecryptBytes() {
        // given
        final var givenEncryptedBytes = Bytes.of(
                new byte[]{-66, 46, -25, 56, -75, -7, -111, 127, -107, -85, 113, 115, -25, -49, 4, -78, -92, -14, -27,
                        -19, -107, 49, -42, 39, 19, -43, 123, -125, 62, 40, 127, -2, 119, 13, 98, 29, -93, -115, 21, 14,
                        -95, -85, 109, 88, -83, -88, 96, 2});
        final var expectedDecryptedBytes = Bytes.of(
                new byte[]{91, 87, 99, 52, 97, 79, 120, 82, 35, 40, 59, 77, 61, 111, 111, 110, 67, 102, 89, 108});

        // when
        final var actual = cryptoProvider.decrypt(givenEncryptedBytes);

        // then
        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isNotNull().isEqualTo(expectedDecryptedBytes);
    }

}
