package de.pflugradts.passbird.adapter.passwordstore;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.NamespaceService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import static de.pflugradts.passbird.adapter.passwordstore.PasswordStoreCommons.EMPTY_NAMESPACE;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.copyBytes;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.copyInt;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.readBytes;
import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.readInt;
import static java.lang.Integer.BYTES;

@NoArgsConstructor
@AllArgsConstructor
class NamespaceTransformer {

    @Inject
    private NamespaceService namespaceService;

    byte[] transform(final NamespaceSlot namespaceSlot) {
        final var namespaceBytes = namespaceService.atSlot(namespaceSlot)
            .map(Namespace::getBytes)
            .orElse(Bytes.emptyBytes());
        final var namespaceBytesSize = namespaceBytes.getSize();
        final var bytes = new byte[BYTES + namespaceBytesSize];
        copyInt(namespaceBytes.isEmpty() ? EMPTY_NAMESPACE : namespaceBytesSize, bytes, 0);
        if (!namespaceBytes.isEmpty()) {
            copyBytes(namespaceBytes.toByteArray(), bytes, BYTES, namespaceBytesSize);
        }
        return bytes;
    }

    Tuple<Bytes, Integer> transform(final byte[] bytes, final int offset) {
        var incrementedOffset = offset;
        final int namespaceSize = readInt(bytes, incrementedOffset);
        incrementedOffset += BYTES;
        final Bytes result;
        if (namespaceSize > 0) {
            final byte[] namespaceBytes = readBytes(bytes, incrementedOffset, namespaceSize);
            incrementedOffset += namespaceBytes.length;
            result = Bytes.bytesOf(namespaceBytes);
        } else {
            result = Bytes.emptyBytes();
        }
        return new Tuple<>(result, incrementedOffset - offset);
    }

}
