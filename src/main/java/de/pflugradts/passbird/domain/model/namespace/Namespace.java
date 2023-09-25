package de.pflugradts.passbird.domain.model.namespace;

import de.pflugradts.passbird.domain.model.transfer.Bytes;

public class Namespace {

    public static final Namespace DEFAULT = new Namespace(Bytes.bytesOf("Default"), NamespaceSlot.DEFAULT);

    private final Bytes bytes;
    private final NamespaceSlot slot;

    private Namespace(final Bytes bytes, final NamespaceSlot namespaceSlot) {
        this.bytes = bytes.copy();
        this.slot = namespaceSlot;
    }

    public Bytes getBytes() {
        return bytes;
    }

    public NamespaceSlot getSlot() {
        return slot;
    }

    static Namespace create(final Bytes bytes, final NamespaceSlot namespaceSlot) {
        return new Namespace(bytes, namespaceSlot);
    }

}
