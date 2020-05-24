package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.util.stream.Stream;

public interface ExchangeAdapterPort {
    Try<Void> send(Stream<Tuple2<Bytes, Bytes>> data);
    Try<Stream<Tuple2<Bytes, Bytes>>> receive();
}
