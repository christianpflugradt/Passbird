package de.pflugradts.pwman3.domain.model.transfer;

import de.pflugradts.pwman3.domain.model.ddd.ValueObject;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.FIRST;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.INVALID;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.LAST;

/**
 * An Input represents data given by the user through the
 * {@link de.pflugradts.pwman3.application.UserInterfaceAdapterPort UserInterface}.
 */
@RequiredArgsConstructor(staticName = "of")
@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class Input implements ValueObject {

    Bytes bytes;

    public static Input empty() {
        return Input.of(Bytes.of());
    }

    public Bytes getCommand() {
        if (bytes.size() > 0) {
            for (int i = 1; i < bytes.size(); i++) {
                if (CharValue.of(bytes.getByte(i)).isAlphabeticCharacter()) {
                    return bytes.slice(0, i);
                }
            }
            return bytes;
        } else {
            return Bytes.empty();
        }
    }

    public Bytes getData() {
        return getBytes().size() > 1
                ? getBytes().slice(getCommand().size(), getBytes().size())
                : Bytes.empty();
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
