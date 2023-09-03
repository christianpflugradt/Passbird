package de.pflugradts.passbird.adapter.passwordstore;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.util.ByteArrayUtils;
import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.NamespaceService;
import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import static de.pflugradts.passbird.adapter.passwordstore.PasswordStoreCommons.EMPTY_NAMESPACE;
import static java.lang.Integer.BYTES;

@NoArgsConstructor
@AllArgsConstructor
class NamespaceTransformer {

    @Inject
    private NamespaceService namespaceService;

    byte[] transform(final NamespaceSlot namespaceSlot) {
        final var namespaceBytes = namespaceService.atSlot(namespaceSlot)
            .map(Namespace::getBytes)
            .orElse(Bytes.empty());
        final var namespaceBytesSize = namespaceBytes.size();
        final var bytes = new byte[BYTES + namespaceBytesSize];
        ByteArrayUtils.copyBytes(namespaceBytes.isEmpty() ? EMPTY_NAMESPACE : namespaceBytesSize, bytes, 0);
        if (!namespaceBytes.isEmpty()) {
            ByteArrayUtils.copyBytes(namespaceBytes, bytes, BYTES);
        }
        return bytes;
    }

    Tuple2<Bytes, Integer> transform(final byte[] bytes, final int offset) {
        var incrementedOffset = offset;
        final int namespaceSize = ByteArrayUtils.readInt(bytes, incrementedOffset);
        incrementedOffset += BYTES;
        final Bytes result;
        if (namespaceSize > 0) {
            final byte[] namespaceBytes = ByteArrayUtils.readBytes(bytes, incrementedOffset, namespaceSize);
            incrementedOffset += namespaceBytes.length;
            result = Bytes.of(namespaceBytes);
        } else {
            result = Bytes.empty();
        }
        return new Tuple2<>(result, incrementedOffset - offset);
    }

}