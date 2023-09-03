package de.pflugradts.passbird.domain.model.transfer;

import java.util.Objects;

/**
 * An Output represents data sent to the user through the
 * {@link de.pflugradts.passbird.application.UserInterfaceAdapterPort UserInterface}.
 */
public class Output {

    private final Bytes bytes;

    private Output(final Bytes bytes) {
        this.bytes = bytes;
    }

    public static Output of(final Bytes bytes) {
        return new Output(bytes);
    }

    public static Output empty() {
        return Output.of(Bytes.of());
    }

    public static Output of(final byte... bytes) {
        return Output.of(Bytes.of(bytes));
    }

    public Bytes getBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Output output = (Output) o;
        return Objects.equals(bytes, output.bytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bytes);
    }
}
