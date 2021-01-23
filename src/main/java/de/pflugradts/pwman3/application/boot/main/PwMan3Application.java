package de.pflugradts.pwman3.application.boot.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.commandhandling.InputHandler;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;

@Singleton
public class PwMan3Application implements Bootable {

    public static final char INTERRUPT = 0x03;

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private InputHandler inputHandler;

    @Override
    public void boot() {
        userInterfaceAdapterPort.sendLineBreak();
        Input input;
        while (!isSigTerm(input = receiveInput())) {
            inputHandler.handleInput(input);
        }
    }

    private Input receiveInput() {
        return userInterfaceAdapterPort
                .receive(Output.of(Bytes.of("Enter command: ")))
                .onFailure(failureCollector::collectInputFailure)
                .getOrElse(Input.empty());
    }

    private boolean isSigTerm(final Input input) {
        return input.getData().isEmpty() && input.getCommand().getFirstByte() == INTERRUPT;
    }

}
