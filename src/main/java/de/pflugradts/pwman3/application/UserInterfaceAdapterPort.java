package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import io.vavr.control.Try;

public interface UserInterfaceAdapterPort {
    Try<Input> receive(Output output);
    default Try<Input> receive() {
        return receive(Output.empty());
    }
    Try<Input> receiveSecurely(Output output);
    default Try<Input> receiveSecurely() {
        return receiveSecurely(Output.empty());
    }
    void send(Output output);
    default void sendLineBreak() {
        send(Output.empty());
    }
}
