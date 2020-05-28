package de.pflugradts.pwman3.domain.model.transfer;

import de.pflugradts.pwman3.domain.model.ddd.ValueObject;
import lombok.RequiredArgsConstructor;
import lombok.Value;

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

    public char getCommandChar() {
        return bytes.size() > 0 ? (char) getBytes().getByte(0) : 0;
    }

    public Bytes getData() {
        return getBytes().size() > 1
                ? getBytes().slice(1, getBytes().size())
                : Bytes.empty();
    }

    public boolean isEmpty() {
        return bytes.isEmpty();
    }

    public void invalidate() {
        getBytes().scramble();
    }

}
