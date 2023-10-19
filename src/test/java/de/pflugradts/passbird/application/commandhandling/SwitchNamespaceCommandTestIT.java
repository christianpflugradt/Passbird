package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.SwitchNamespaceCommandHandler;
import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.FixedNamespaceService;
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
import static de.pflugradts.passbird.domain.service.NamespaceServiceTestFactoryKt.createNamespaceServiceForTesting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SwitchNamespaceCommandTestIT {

    private InputHandler inputHandler;
    @Spy
    private final FixedNamespaceService namespaceService = createNamespaceServiceForTesting();
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private SwitchNamespaceCommandHandler switchNamespaceCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(switchNamespaceCommandHandler);
    }

    @Test
    void shouldHandleSwitchNamespaceCommand() {
        // given
        final var givenNamespace = NamespaceSlot.N1;
        namespaceService.deploy(Bytes.bytesOf("namespace"), givenNamespace);
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n" + givenNamespace.index()));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should(never()).send(any());
        assertThat(namespaceService.getCurrentNamespace()).isNotNull()
            .extracting(Namespace::getSlot).isNotNull()
            .isEqualTo(givenNamespace);
    }

    @Test
    void shouldHandleSwitchNamespaceCommand_DoNothingIfNamespaceIsAlreadyCurrent() {
        // given
        final var givenNamespace = NamespaceSlot.N1;
        namespaceService.deploy(Bytes.bytesOf("namespace"), givenNamespace);
        namespaceService.updateCurrentNamespace(givenNamespace);
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n" + givenNamespace.index()));

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
        final var givenNamespace = NamespaceSlot.N1;
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n" + givenNamespace.index()));
        assertThat(namespaceService.atSlot(givenNamespace)).isNotPresent();

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
