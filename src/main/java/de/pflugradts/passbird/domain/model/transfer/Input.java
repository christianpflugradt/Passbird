package de.pflugradts.passbird.domain.model.transfer;

import de.pflugradts.passbird.domain.model.ddd.ValueObject;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import io.vavr.control.Try;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.FIRST;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.INVALID;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.LAST;

/**
 * An Input represents data given by the user through the
 * {@link de.pflugradts.passbird.application.UserInterfaceAdapterPort UserInterface}.
 */
public class Input implements ValueObject {

    private final Bytes bytes;

    public static Input empty() {
        return Input.of(Bytes.emptyBytes());
    }

    private Input(final Bytes bytes) {
        this.bytes = bytes;
    }

    public static Input of(final Bytes bytes) {
        return new Input(bytes);
    }

    public Bytes getBytes() {
        return bytes;
    }

    public Bytes getCommand() {
        if (bytes.getSize() > 0) {
            for (int i = 1; i < bytes.getSize(); i++) {
                if (CharValue.Companion.charValueOf(bytes.getByte(i)).isAlphabeticCharacter()) {
                    return bytes.slice(0, i);
                }
            }
            return bytes;
        } else {
            return Bytes.emptyBytes();
        }
    }

    public Bytes getData() {
        return getBytes().getSize() > 1
                ? getBytes().slice(getCommand().getSize(), getBytes().getSize())
                : Bytes.emptyBytes();
    }

    public NamespaceSlot parseNamespace() {
        return Try.of(() -> Integer.parseInt(getBytes().asString()))
            .map(value -> value >= FIRST - 1 && value <= LAST ? NamespaceSlot.at(value) : INVALID)
            .getOrElse(INVALID);
    }

    public boolean isEmpty() {
        return bytes.isEmpty();
    }

    public void invalidate() {
        getBytes().scramble();
    }

}
