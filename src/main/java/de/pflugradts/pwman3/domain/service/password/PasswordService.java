package de.pflugradts.pwman3.domain.service.password;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>A PasswordServices manages {@link de.pflugradts.pwman3.domain.model.password.PasswordEntry PasswordEntries}.</p>
 * <p>PasswordEntries can be viewed, created, updated or removed through methods such as {@link #viewPassword(Bytes)},
 * {@link #putPasswordEntry(Bytes, Bytes)} and {@link #discardPasswordEntry(Bytes)}.</p>
 */
public interface PasswordService {
    Try<Boolean> entryExists(Bytes keyBytes);
    Optional<Try<Bytes>> viewPassword(Bytes keyBytes);
    Try<Void> putPasswordEntries(Stream<Tuple2<Bytes, Bytes>> passwordEntries);
    Try<Void> putPasswordEntry(Bytes keyBytes, Bytes passwordBytes);
    Try<Void> discardPasswordEntry(Bytes keyBytes);
    Try<Stream<Bytes>> findAllKeys();
}
