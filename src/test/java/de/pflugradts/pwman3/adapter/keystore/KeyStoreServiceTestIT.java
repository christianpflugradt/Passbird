package de.pflugradts.pwman3.adapter.keystore;

import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Chars;
import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static de.pflugradts.pwman3.application.util.CryptoUtils.KEYSTORE_KEY_BITS;
import static org.assertj.core.api.Assertions.assertThat;

class KeyStoreServiceTestIT {

    private KeyStoreService keyStoreService;

    private String tempKeyStoreDirectory;
    private String keyStoreFile;

    @BeforeEach
    private void setup() {
        keyStoreService = new KeyStoreService(new SystemOperation());
        tempKeyStoreDirectory = UUID.randomUUID().toString();
        keyStoreFile = tempKeyStoreDirectory + File.separator + ReadableConfiguration.KEYSTORE_FILENAME;
        assertThat(new File(tempKeyStoreDirectory).mkdir()).isTrue();
    }

    @AfterEach
    private void cleanup() {
        assertThat(new File(keyStoreFile).delete()).isTrue();
        assertThat(new File(tempKeyStoreDirectory).delete()).isTrue();
    }

    @Test
    void shouldUseKeyStore_Roundtrip() {
        // given
        final var password = "p4s5wrD";
        final var oneTimePasswordChars1 = Chars.of(password.toCharArray());
        final var oneTimePasswordChars2 = Chars.of(password.toCharArray());
        final var path = Paths.get(keyStoreFile);
        final var expectedByteArraySize = KEYSTORE_KEY_BITS / 8;

        assertThat(oneTimePasswordChars1.toCharArray()).isEqualTo(password.toCharArray());
        assertThat(oneTimePasswordChars2.toCharArray()).isEqualTo(password.toCharArray());

        // when

        keyStoreService.storeKey(oneTimePasswordChars1, path);

        final var actual = keyStoreService.loadKey(oneTimePasswordChars2, path);

        // then
        assertThat(new File(keyStoreFile)).exists();
        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isNotNull();
        assertThat(actual.get().getSecret()).isNotNull().hasSize(expectedByteArraySize);
        assertThat(actual.get().getIv()).isNotNull().hasSize(expectedByteArraySize);

        assertThat(oneTimePasswordChars1.toCharArray()).isNotEqualTo(password.toCharArray());
        assertThat(oneTimePasswordChars2.toCharArray()).isNotEqualTo(password.toCharArray());
    }

}
