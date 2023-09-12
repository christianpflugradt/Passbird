package de.pflugradts.passbird.application;

import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.transfer.Bytes;

import java.util.stream.Stream;

/**
 * AdapterPort for exchanging password data with a 3rd party.
 */
public interface ExchangeAdapterPort {
    void send(Stream<Tuple<Bytes, Bytes>> data);
    Stream<Tuple<Bytes, Bytes>> receive();
}
