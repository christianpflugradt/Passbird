package de.pflugradts.pwman3.adapter.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.pflugradts.pwman3.adapter.exchange.model.PasswordEntriesRepresentation;
import de.pflugradts.pwman3.application.ExchangeAdapterPort;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.EXCHANGE_FILENAME;

@AllArgsConstructor
public class FilePasswordExchange implements ExchangeAdapterPort {

    @Inject
    private PasswordEntryTransformer passwordEntryTransformer;

    private final String uri;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public FilePasswordExchange(@Assisted final String uri) {
        this.uri = uri;
    }

    @Override
    public Try<Void> send(final Stream<Tuple2<Bytes, Bytes>> data) {
        return Try.run(() -> Files.writeString(
                Paths.get(uri).resolve(EXCHANGE_FILENAME),
                objectMapper.writeValueAsString(new PasswordEntriesRepresentation(data
                        .map(passwordEntryTransformer::transform)
                        .collect(Collectors.toList())))));
    }

    @Override
    public Try<Stream<Tuple2<Bytes, Bytes>>> receive() {
        return Try.of(() -> objectMapper.readValue(
                        Files.readString(Paths.get(uri).resolve(EXCHANGE_FILENAME)),
                        PasswordEntriesRepresentation.class)
                .getPasswordEntryRepresentations()
                .stream()
                .map(passwordEntryTransformer::transform));
    }

}
