package de.pflugradts.passbird.adapter.passwordstore;

import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;

import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.copyBytes;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.copyInt;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.readBytes;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.readInt;
import static java.lang.Integer.BYTES;

class PasswordEntryTransformer {

    byte[] transform(final PasswordEntry passwordEntry) {
        final var keySize = passwordEntry.viewKey().getSize();
        final var passwordSize = passwordEntry.viewPassword().getSize();
        final var metaSize = 2 * BYTES;
        final var bytes = new byte[BYTES + keySize + passwordSize + metaSize];
        var offset = copyInt(passwordEntry.associatedNamespace().index(), bytes, 0);
        offset += copyInt(keySize, bytes, offset);
        offset += copyBytes(passwordEntry.viewKey().toByteArray(), bytes, offset, keySize);
        offset += copyInt(passwordSize, bytes, offset);
        copyBytes(passwordEntry.viewPassword().toByteArray(), bytes, offset, passwordSize);
        return bytes;
    }

    Tuple<PasswordEntry, Integer> transform(final byte[] bytes, final int offset, final boolean legacyMode) {
        var incrementedOffset = offset;
        final int namespaceSlot = legacyMode
            ? NamespaceSlot.DEFAULT.index()
            : readInt(bytes, incrementedOffset);
        incrementedOffset += legacyMode ? 0 : BYTES;
        final int keySize = readInt(bytes, incrementedOffset);
        incrementedOffset += BYTES;
        final byte[] keyBytes = readBytes(bytes, incrementedOffset, keySize);
        incrementedOffset += keySize;
        final int passwordSize = readInt(bytes, incrementedOffset);
        incrementedOffset += BYTES;
        final byte[] passwordBytes = readBytes(bytes, incrementedOffset, passwordSize);
        incrementedOffset += passwordSize;
        return new Tuple<>(
                PasswordEntry.create(
                    NamespaceSlot.at(namespaceSlot),
                    Bytes.bytesOf(keyBytes),
                    Bytes.bytesOf(passwordBytes)),
                incrementedOffset);
    }

}
