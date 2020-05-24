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

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private InputHandler inputHandler;

    @Override
    public void boot() {
        userInterfaceAdapterPort.sendLineBreak();
        while (true) {
            inputHandler.handleInput(
                    userInterfaceAdapterPort
                            .receive(Output.of(Bytes.of("Enter command: ")))
                            .onFailure(failureCollector::acceptInputFailure)
                            .getOrElse(Input.empty())
            );
        }
    }

}
