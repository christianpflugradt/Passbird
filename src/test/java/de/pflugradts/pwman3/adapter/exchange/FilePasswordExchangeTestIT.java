package de.pflugradts.pwman3.adapter.exchange;

import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import java.io.File;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FilePasswordExchangeTestIT {

    private FilePasswordExchange filePasswordExchange;

    private String tempExchangeDirectory;
    private String exchangeFile;

    @BeforeEach
    private void setup() {
        tempExchangeDirectory = UUID.randomUUID().toString();
        exchangeFile = tempExchangeDirectory + File.separator + ReadableConfiguration.EXCHANGE_FILENAME;
        assertThat(new File(tempExchangeDirectory).mkdir()).isTrue();
        filePasswordExchange = new FilePasswordExchange(new PasswordEntryTransformer(), tempExchangeDirectory);
    }

    @AfterEach
    private void cleanup() {
        assertThat(new File(exchangeFile).delete()).isTrue();
        assertThat(new File(tempExchangeDirectory).delete()).isTrue();
    }

    @Test
    void shouldUseFileSystem_Roundtrip() {
        // given
        final var givenPasswordEntry1 = new Tuple2<>(Bytes.of("key1"), Bytes.of("password1"));
        final var givenPasswordEntry2 = new Tuple2<>(Bytes.of("key2"), Bytes.of("password2"));
        final var givenPasswordEntry3 = new Tuple2<>(Bytes.of("key3"), Bytes.of("password3"));

        // when
        filePasswordExchange.send(Stream.of(givenPasswordEntry1, givenPasswordEntry2, givenPasswordEntry3));
        final var actual = filePasswordExchange.receive();

        // then
        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).containsExactlyInAnyOrder(
                givenPasswordEntry1,
                givenPasswordEntry2,
                givenPasswordEntry3);
    }

}
