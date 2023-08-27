package de.pflugradts.passbird.adapter.userinterface;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Chars;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.ArrayList;

@Singleton
public class CommandLineInterfaceService implements UserInterfaceAdapterPort {

    @Inject
    private SystemOperation systemOperation;
    @Inject
    private ReadableConfiguration configuration;

    @Override
    public Try<Input> receive(final Output output) {
        sendWithoutLineBreak(output);
        return Try.of(this::receivePlain);
    }

    private Input receivePlain() throws IOException {
        final var bytes = new ArrayList<Byte>();
        char next;
        while (!isLinebreak(next = in())) {
            bytes.add((byte) next);
        }
        return Input.of(Bytes.of(bytes));
    }

    private char in() throws IOException {
        return (char) System.in.read();
    }

    private boolean isLinebreak(final char chr) {
        return chr == '\n';
    }

    @Override
    public Try<Input> receiveSecurely(final Output output) {
        sendWithoutLineBreak(output);
        return configuration.getAdapter().getUserInterface().isSecureInput() && systemOperation.isConsoleAvailable()
                ? systemOperation.readPasswordFromConsole().map(charArray -> Input.of(Chars.of(charArray).toBytes()))
                : receive();
    }

    private void sendWithoutLineBreak(final Output output) {
        Bytes.of(output.getBytes().toByteArray()).forEach(b -> sendChar((char) b.byteValue()));
    }

    private void sendWithLineBreak(final Output output) {
        sendWithoutLineBreak(output);
        sendChar('\n');
    }

    private void sendChar(final char chr) {
        System.out.print(chr);
    }

    @Override
    public void send(final Output output) {
        sendWithLineBreak(output);
    }

}
