package de.pflugradts.passbird.application.boot.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.commandhandling.InputHandler;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.NamespaceService;

import static de.pflugradts.passbird.domain.model.namespace.Namespace.DEFAULT;

@Singleton
public class PassbirdApplication implements Bootable {

    public static final char INTERRUPT = 0x03;

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private NamespaceService namespaceService;
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
                .receive(Output.Companion.outputOf(Bytes.bytesOf(namespacePrefix() + "Enter command: ")));
    }

    private String namespacePrefix() {
        final var currentNamespace = namespaceService.getCurrentNamespace();
        return currentNamespace.equals(DEFAULT) ? "" : "[" + currentNamespace.getBytes().asString() + "] ";
    }

    private boolean isSigTerm(final Input input) {
        return input.getData().isEmpty()
            && !input.getCommand().isEmpty()
            && input.getCommand().getFirstByte() == INTERRUPT;
    }

}
