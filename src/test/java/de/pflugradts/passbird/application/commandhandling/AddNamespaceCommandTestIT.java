package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AddNamespaceCommandHandler;
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

import java.util.Optional;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static de.pflugradts.passbird.domain.service.NamespaceServiceTestFactoryKt.createNamespaceServiceForTesting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AddNamespaceCommandTestIT {

    private InputHandler inputHandler;
    @Spy
    private final FixedNamespaceService namespaceService = createNamespaceServiceForTesting();
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private AddNamespaceCommandHandler addNamespaceCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(addNamespaceCommandHandler);
    }

    @Test
    void shouldHandleAddNamespaceCommand() {
        // given
        final var slotIndex = 1;
        final var givenInput = Bytes.bytesOf("n+" + slotIndex);
        final var slotFromInput = NamespaceSlot.Companion.at(slotIndex);
        final var referenceNamespace = Bytes.bytesOf("mynamespace");
        final var givenNamespace = Bytes.bytesOf("mynamespace");
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs( Input.Companion.inputOf(givenNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(userInterfaceAdapterPort).should(never()).send(any());
        assertNamespaceEquals(namespaceService.atSlot(slotFromInput), referenceNamespace);
        assertThat(givenNamespace).isNotNull().isNotEqualTo(referenceNamespace);
    }

    @Test
    void shouldHandleAddNamespaceCommand_UpdateExistingNamespace() {
        // given
        final var slotIndex = 1;
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n+" + slotIndex));
        final var slotFromInput = NamespaceSlot.Companion.at(slotIndex);
        final var referenceNamespace = Bytes.bytesOf("mynamespace");
        final var givenNamespace = Bytes.bytesOf("mynamespace");
        final var otherNamespace = Bytes.bytesOf("othernamespace");
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs( Input.Companion.inputOf(givenNamespace)).fake();

        namespaceService.deploy(otherNamespace, slotFromInput);
        assertNamespaceEquals(namespaceService.atSlot(slotFromInput), otherNamespace);

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should(never()).send(any());
        assertNamespaceEquals(namespaceService.atSlot(slotFromInput), referenceNamespace);
        assertThat(givenNamespace).isNotNull().isNotEqualTo(referenceNamespace);
    }

    @Test
    void shouldHandleAddNamespaceCommand_EmptyInput() {
        // given
        final var slotIndex = 1;
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n+" + slotIndex));
        final var slotFromInput = NamespaceSlot.Companion.at(slotIndex);
        final var givenNamespace = Bytes.bytesOf("");
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs( Input.Companion.inputOf(givenNamespace)).fake();

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull()
            .asString().contains("Empty input");
        assertThat(namespaceService.atSlot(slotFromInput)).isNotPresent();
    }

    @Test
    void shouldHandleAddNamespaceCommand_DefaultSlot() {
        // given
        final var slotIndex = 0;
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("n+" + slotIndex));
        final var slotFromInput = NamespaceSlot.Companion.at(slotIndex);

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should(never()).receive(any());
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull()
            .asString().contains("Default namespace cannot be replaced");
    }

    private static void assertNamespaceEquals(final Optional<Namespace> deployedNamespace, final Bytes expectedNamespaceBytes) {
        assertThat(deployedNamespace)
            .isNotEmpty().get()
            .extracting(Namespace::getBytes).isNotNull()
            .isEqualTo(expectedNamespaceBytes);
    }


}
