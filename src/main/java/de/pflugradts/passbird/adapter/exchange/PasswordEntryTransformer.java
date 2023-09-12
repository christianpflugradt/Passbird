package de.pflugradts.passbird.adapter.exchange;

import de.pflugradts.passbird.adapter.exchange.model.PasswordEntryRepresentation;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.transfer.Bytes;

class PasswordEntryTransformer {

    Tuple<Bytes, Bytes> transform(final PasswordEntryRepresentation passwordEntryRepresentation) {
        return new Tuple<>(
                Bytes.bytesOf(passwordEntryRepresentation.getKey()),
                Bytes.bytesOf(passwordEntryRepresentation.getPassword()));
    }

    PasswordEntryRepresentation transform(final Tuple<Bytes, Bytes> tuple) {
        return new PasswordEntryRepresentation(
                tuple.get_1().asString(),
                tuple.get_2().asString());
    }

}
