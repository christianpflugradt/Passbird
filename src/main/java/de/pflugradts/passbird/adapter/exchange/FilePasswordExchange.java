package de.pflugradts.passbird.adapter.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.pflugradts.passbird.adapter.exchange.model.PasswordEntriesRepresentation;
import de.pflugradts.passbird.application.ExchangeAdapterPort;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;

import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.pflugradts.passbird.application.configuration.ReadableConfiguration.EXCHANGE_FILENAME;

@AllArgsConstructor
public class FilePasswordExchange implements ExchangeAdapterPort {

    @Inject
    private PasswordEntryTransformer passwordEntryTransformer;
    @Inject
    private SystemOperation systemOperation;

    private final String uri;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public FilePasswordExchange(@Assisted final String uri) {
        this.uri = uri;
    }

    @Override
    public Try<Void> send(final Stream<Tuple2<Bytes, Bytes>> data) {
        return Try.run(() -> Files.writeString(
                systemOperation.resolvePath(uri, EXCHANGE_FILENAME),
                objectMapper.writeValueAsString(new PasswordEntriesRepresentation(data
                        .map(passwordEntryTransformer::transform)
                        .collect(Collectors.toList())))));
    }

    @Override
    public Try<Stream<Tuple2<Bytes, Bytes>>> receive() {
        return Try.of(() -> objectMapper.readValue(
                        Files.readString(systemOperation.resolvePath(uri, EXCHANGE_FILENAME)),
                        PasswordEntriesRepresentation.class)
                .getPasswordEntryRepresentations()
                .stream()
                .map(passwordEntryTransformer::transform));
    }

}
