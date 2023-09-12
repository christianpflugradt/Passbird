package de.pflugradts.passbird.adapter.passwordstore;

import de.pflugradts.passbird.application.util.ByteArrayUtils;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;

import static java.lang.Integer.BYTES;

class PasswordEntryTransformer {

    byte[] transform(final PasswordEntry passwordEntry) {
        final var keySize = passwordEntry.viewKey().getSize();
        final var passwordSize = passwordEntry.viewPassword().getSize();
        final var metaSize = 2 * BYTES;
        final var bytes = new byte[BYTES + keySize + passwordSize + metaSize];
        var offset = ByteArrayUtils.copyBytes(passwordEntry.associatedNamespace().index(), bytes, 0);
        offset += ByteArrayUtils.copyBytes(keySize, bytes, offset);
        offset += ByteArrayUtils.copyBytes(passwordEntry.viewKey(), bytes, offset, keySize);
        offset += ByteArrayUtils.copyBytes(passwordSize, bytes, offset);
        ByteArrayUtils.copyBytes(passwordEntry.viewPassword(), bytes, offset, passwordSize);
        return bytes;
    }

    Tuple<PasswordEntry, Integer> transform(final byte[] bytes, final int offset, final boolean legacyMode) {
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
        return new Tuple<>(
                PasswordEntry.create(
                    NamespaceSlot.at(namespaceSlot),
                    Bytes.bytesOf(keyBytes),
                    Bytes.bytesOf(passwordBytes)),
                incrementedOffset);
    }

}
