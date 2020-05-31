package de.pflugradts.pwman3.adapter.exchange;

import de.pflugradts.pwman3.adapter.exchange.model.PasswordEntryRepresentation;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;

class PasswordEntryTransformer {

    Tuple2<Bytes, Bytes> transform(final PasswordEntryRepresentation passwordEntryRepresentation) {
        return new Tuple2<>(
                Bytes.of(passwordEntryRepresentation.getKey()),
                Bytes.of(passwordEntryRepresentation.getPassword()));
    }

    PasswordEntryRepresentation transform(final Tuple2<Bytes, Bytes> tuple) {
        return new PasswordEntryRepresentation(
                tuple._1().asString(),
                tuple._2().asString());
    }

}
