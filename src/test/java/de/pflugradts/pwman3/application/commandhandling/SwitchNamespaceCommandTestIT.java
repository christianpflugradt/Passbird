package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.handler.namespace.SwitchNamespaceCommandHandler;
import de.pflugradts.pwman3.domain.model.namespace.Namespace;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.NamespaceServiceFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.pwman3.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SwitchNamespaceCommandTestIT {

    private InputHandler inputHandler;
    @Spy
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private SwitchNamespaceCommandHandler switchNamespaceCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    private void setup() {
        inputHandler = setupInputHandlerFor(switchNamespaceCommandHandler);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldHandleSwitchNamespaceCommand() {
        // given
        final var givenNamespace = NamespaceSlot._1;
        namespaceServiceFake.deploy(Bytes.of("namespace1"), givenNamespace);
        final var input = Input.of(Bytes.of("n" + givenNamespace.index()));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should(never()).send(any());
        assertThat(namespaceServiceFake.getCurrentNamespace()).isNotNull()
            .extracting(Namespace::getSlot).isNotNull()
            .isEqualTo(givenNamespace);
    }

    @Test
    void shouldHandleSwitchNamespaceCommand_DoNothingIfNamespaceIsAlreadyCurrent() {
        // given
        final var givenNamespace = NamespaceSlot._1;
        namespaceServiceFake.deploy(Bytes.of("namespace1"), givenNamespace);
        namespaceServiceFake.updateCurrentNamespace(givenNamespace);
        final var input = Input.of(Bytes.of("n" + givenNamespace.index()));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull()
            .asString().contains("is already the current namespace");
    }

    @Test
    void shouldHandleSwitchNamespaceCommand_DoNothingIfNamespaceIsNotDeployed() {
        // given
        final var givenNamespace = NamespaceSlot._1;
        final var input = Input.of(Bytes.of("n" + givenNamespace.index()));
        assertThat(namespaceServiceFake.atSlot(givenNamespace)).isNotPresent();

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull()
            .asString().contains("Specified namespace does not exist");
    }

}
