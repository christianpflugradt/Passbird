package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.ViewNamespaceCommandHandler;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.NamespaceServiceFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ViewNamespaceCommandTestIT {

    private InputHandler inputHandler;
    @Spy
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private ViewNamespaceCommandHandler viewNamespaceCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(viewNamespaceCommandHandler);
    }

    @Test
    void shouldHandleViewNamespaceCommand_PrintInfo() {
        // given
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n"));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull()
                .asString().contains("Available namespace commands");
    }

    @Test
    void shouldHandleViewNamespaceCommand_PrintDefaultNamespaceIfCurrent() {
        // given
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n"));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull()
            .asString().contains("Current namespace: Default");
    }

    @Test
    void shouldHandleViewNamespaceCommand_PrintDeployedNamespace() {
        // given
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n"));
        final var deployedNamespaceSlot = 3;
        final var deployedNamespace = "mynamespace";
        namespaceServiceFake.deploy(Bytes.bytesOf(deployedNamespace), NamespaceSlot.Companion.at(deployedNamespaceSlot));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull()
            .asString().contains(deployedNamespaceSlot + ": " + deployedNamespace);
    }

}
