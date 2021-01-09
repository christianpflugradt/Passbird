package de.pflugradts.pwman3.adapter.passwordstore;

import de.pflugradts.pwman3.application.util.ByteArrayUtils;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;

import static java.lang.Integer.BYTES;

class PasswordEntryTransformer {

    byte[] transform(final PasswordEntry passwordEntry) {
        final var keySize = passwordEntry.viewKey().size();
        final var passwordSize = passwordEntry.viewPassword().size();
        final var metaSize = 2 * BYTES;
        final var bytes = new byte[BYTES + keySize + passwordSize + metaSize];
        var offset = ByteArrayUtils.copyBytes(passwordEntry.associatedNamespace().index(), bytes, 0);
        offset += ByteArrayUtils.copyBytes(keySize, bytes, offset);
        offset += ByteArrayUtils.copyBytes(passwordEntry.viewKey(), bytes, offset, keySize);
        offset += ByteArrayUtils.copyBytes(passwordSize, bytes, offset);
        ByteArrayUtils.copyBytes(passwordEntry.viewPassword(), bytes, offset, passwordSize);
        return bytes;
    }

    Tuple2<PasswordEntry, Integer> transform(final byte[] bytes, final int offset, final boolean legacyMode) {
        var incrementedOffset = offset;
        final int namespaceSlot = legacyMode
            ? NamespaceSlot.DEFAULT.index()
            : ByteArrayUtils.readInt(bytes, incrementedOffset);
        incrementedOffset += legacyMode ? 0 : BYTES;
        final int keySize = ByteArrayUtils.readInt(bytes, incrementedOffset);
        incrementedOffset += BYTES;
        final byte[] keyBytes = ByteArrayUtils.readBytes(bytes, incrementedOffset, keySize);
        incrementedOffset += keySize;
        final int passwordSize = ByteArrayUtils.readInt(bytes, incrementedOffset);
        incrementedOffset += BYTES;
        final byte[] passwordBytes = ByteArrayUtils.readBytes(bytes, incrementedOffset, passwordSize);
        incrementedOffset += passwordSize;
        return new Tuple2<>(
                PasswordEntry.create(
                    NamespaceSlot.at(namespaceSlot),
                    Bytes.of(keyBytes),
                    Bytes.of(passwordBytes)),
                incrementedOffset);
    }

}
