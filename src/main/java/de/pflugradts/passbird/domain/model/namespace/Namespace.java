package de.pflugradts.passbird.domain.model.namespace;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.Getter;

@Getter
public class Namespace {

    public static final Namespace DEFAULT = new Namespace(Bytes.of("Default"), NamespaceSlot.DEFAULT);

    private final Bytes bytes;
    private final NamespaceSlot slot;

    private Namespace(final Bytes bytes, final NamespaceSlot namespaceSlot) {
        this.bytes = bytes.copy();
        this.slot = namespaceSlot;
    }

    static Namespace create(final Bytes bytes, final NamespaceSlot namespaceSlot) {
        return new Namespace(bytes, namespaceSlot);
    }

}
