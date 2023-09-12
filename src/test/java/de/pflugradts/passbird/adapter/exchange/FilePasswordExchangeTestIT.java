package de.pflugradts.passbird.adapter.exchange;

import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FilePasswordExchangeTestIT {

    private FilePasswordExchange filePasswordExchange;

    private String tempExchangeDirectory;
    private String exchangeFile;

    @BeforeEach
    void setup() {
        tempExchangeDirectory = UUID.randomUUID().toString();
        exchangeFile = tempExchangeDirectory + File.separator + ReadableConfiguration.EXCHANGE_FILENAME;
        assertThat(new File(tempExchangeDirectory).mkdir()).isTrue();
        filePasswordExchange = new FilePasswordExchange(
                new PasswordEntryTransformer(),
                new SystemOperation(),
                tempExchangeDirectory);
    }

    @AfterEach
    void cleanup() {
        assertThat(new File(exchangeFile).delete()).isTrue();
        assertThat(new File(tempExchangeDirectory).delete()).isTrue();
    }

    @Test
    void shouldUseFileSystem_Roundtrip() {
        // given
        final var givenPasswordEntry1 = new Tuple<>(Bytes.bytesOf("key1"), Bytes.bytesOf("password1"));
        final var givenPasswordEntry2 = new Tuple<>(Bytes.bytesOf("key2"), Bytes.bytesOf("password2"));
        final var givenPasswordEntry3 = new Tuple<>(Bytes.bytesOf("key3"), Bytes.bytesOf("password3"));

        // when
        filePasswordExchange.send(Stream.of(givenPasswordEntry1, givenPasswordEntry2, givenPasswordEntry3));
        final var actual = filePasswordExchange.receive();

        // then
        assertThat(actual).containsExactlyInAnyOrder(
                givenPasswordEntry1,
                givenPasswordEntry2,
                givenPasswordEntry3);
    }

}
