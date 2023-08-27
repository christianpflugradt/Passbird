package de.pflugradts.passbird.application;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.util.stream.Stream;

/**
 * AdapterPort for exchanging password data with a 3rd party.
 */
public interface ExchangeAdapterPort {
    Try<Void> send(Stream<Tuple2<Bytes, Bytes>> data);
    Try<Stream<Tuple2<Bytes, Bytes>>> receive();
}
