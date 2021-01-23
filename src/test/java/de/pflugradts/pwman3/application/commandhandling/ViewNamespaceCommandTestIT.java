package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.NamespaceCommandFactory;
import de.pflugradts.pwman3.application.commandhandling.handler.namespace.ViewNamespaceCommandHandler;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ViewNamespaceCommandTestIT {

    private InputHandler inputHandler;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private ViewNamespaceCommandHandler viewNamespaceCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    private void setup() {
        inputHandler = new InputHandler(
                new CommandBus(null, Set.of(viewNamespaceCommandHandler)),
                new CommandFactory(new NamespaceCommandFactory()));
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldHandleViewNamespaceCommand_PrintInfo() {
        // given
        final var input = Input.of(Bytes.of("n"));

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
    void shouldHandleViewNamespaceCommand_PrintDefaultNamespace() {
        // given
        final var input = Input.of(Bytes.of("n"));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull()
            .asString().contains("Current namespace: default");
    }

}
