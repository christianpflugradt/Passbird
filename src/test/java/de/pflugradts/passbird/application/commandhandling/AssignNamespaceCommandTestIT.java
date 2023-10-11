package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AssignNamespaceCommandHandler;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.NamespaceServiceFake;
import de.pflugradts.passbird.domain.service.PasswordServiceFaker;
import de.pflugradts.passbird.domain.service.password.PasswordService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AssignNamespaceCommandTestIT {

    private InputHandler inputHandler;
    @Spy
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    @Mock
    private PasswordService passwordService;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private FailureCollector failureCollector;
    @InjectMocks
    private AssignNamespaceCommandHandler assignNamespaceCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(assignNamespaceCommandHandler);
    }

    @Test
    void shouldHandleAssignNamespaceCommand() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var referenceInput = givenInput.copy();
        final var expectedNamespace = 1;

        namespaceServiceFake.deployAtIndex(expectedNamespace);
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(
                PasswordEntryFaker.faker()
                    .fakePasswordEntry()
                    .withKeyBytes(Bytes.bytesOf(givenAlias)).fake()).fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(expectedNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(passwordService).should().movePasswordEntry(Bytes.bytesOf(givenAlias), NamespaceSlot.Companion.at(expectedNamespace));
        assertThat(givenInput).isNotNull().isNotEqualTo(referenceInput);
    }

    @Test
    void shouldHandleAssignNamespaceCommand_EntryNotExists() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var referenceInput = givenInput.copy();
        final var expectedNamespace = 1;

        namespaceServiceFake.deployAtIndex(expectedNamespace);
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries().fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(expectedNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(passwordService).should(never()).movePasswordEntry(any(Bytes.class), any(NamespaceSlot.class));
        assertThat(givenInput).isNotNull().isNotEqualTo(referenceInput);
    }

    @Test
    void shouldHandleAssignNamespaceCommand_DisplayTargetNamespace() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var currentNamespace = 0;
        final var targetNamespace = 1;

        namespaceServiceFake.updateCurrentNamespace(NamespaceSlot.Companion.at(currentNamespace));
        namespaceServiceFake.deployAtIndex(targetNamespace);
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(
                PasswordEntryFaker.faker()
                    .fakePasswordEntry()
                    .withKeyBytes(Bytes.bytesOf(givenAlias))
                    .withNamespace(NamespaceSlot.Companion.at(currentNamespace)).fake()).fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(targetNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().asString()
            .contains(targetNamespace + ": ")
            .doesNotContain(currentNamespace + ": ");
    }

    @Test
    void shouldHandleAssignNamespaceCommand_DisplayTargetNamespaceWhenNotDefault() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var currentNamespace = 1;
        final var targetNamespace = 0;

        namespaceServiceFake.deployAtIndex(currentNamespace);
        namespaceServiceFake.updateCurrentNamespace(NamespaceSlot.Companion.at(currentNamespace));
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(
                PasswordEntryFaker.faker()
                    .fakePasswordEntry()
                    .withKeyBytes(Bytes.bytesOf(givenAlias))
                    .withNamespace(NamespaceSlot.Companion.at(currentNamespace)).fake()).fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(targetNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().asString()
            .contains(targetNamespace + ": ")
            .doesNotContain(currentNamespace + ": ");
    }

    @Test
    void shouldHandleAssignNamespaceCommand_EnteredInvalidNamespace() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var referenceInput = givenInput.copy();
        final var invalidNamespace = -1;

        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(
                PasswordEntryFaker.faker()
                    .fakePasswordEntry()
                    .withKeyBytes(Bytes.bytesOf(givenAlias)).fake()).fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(invalidNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(userInterfaceAdapterPort).should(times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(1)).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().asString()
            .contains("Invalid namespace");
        then(passwordService).should(never()).movePasswordEntry(any(Bytes.class), any(NamespaceSlot.class));
        assertThat(givenInput).isNotNull().isNotEqualTo(referenceInput);
    }

    @Test
    void shouldHandleAssignNamespaceCommand_EnteredCurrentNamespace() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var referenceInput = givenInput.copy();
        final var currentNamespace = 1;

        namespaceServiceFake.deployAtIndex(currentNamespace);
        namespaceServiceFake.updateCurrentNamespace(NamespaceSlot.Companion.at(currentNamespace));
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(
                PasswordEntryFaker.faker()
                    .fakePasswordEntry()
                    .withKeyBytes(Bytes.bytesOf(givenAlias)).fake()).fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(currentNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(userInterfaceAdapterPort).should(times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(1)).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().asString()
            .contains("Password entry is already in the specified namespace");
        then(passwordService).should(never()).movePasswordEntry(any(Bytes.class), any(NamespaceSlot.class));
        assertThat(givenInput).isNotNull().isNotEqualTo(referenceInput);
    }

    @Test
    void shouldHandleAssignNamespaceCommand_EnteredEmptyNamespace() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var referenceInput = givenInput.copy();
        final var targetNamespace = 1;

        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(
                PasswordEntryFaker.faker()
                    .fakePasswordEntry()
                    .withKeyBytes(Bytes.bytesOf(givenAlias)).fake()).fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(targetNamespace)).fake();
        assertThat(namespaceServiceFake.atSlot(NamespaceSlot.Companion.at(targetNamespace))).isNotPresent();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(userInterfaceAdapterPort).should(times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(1)).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().asString()
            .contains("Specified namespace does not exist");
        then(passwordService).should(never()).movePasswordEntry(any(Bytes.class), any(NamespaceSlot.class));
        assertThat(givenInput).isNotNull().isNotEqualTo(referenceInput);
    }

    @Test
    void shouldHandleAssignNamespaceCommand_OtherPasswordWithSameAliasAlreadyInTargetNamespace() {
        // given
        final var givenAlias = "a";
        final var givenInput = Bytes.bytesOf("n" + givenAlias);
        final var referenceInput = givenInput.copy();
        final var currentNamespace = 0;
        final var targetNamespace = 1;

        namespaceServiceFake.updateCurrentNamespace(NamespaceSlot.Companion.at(currentNamespace));
        namespaceServiceFake.deployAtIndex(targetNamespace);
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(
                PasswordEntryFaker.faker()
                    .fakePasswordEntry()
                    .withKeyBytes(Bytes.bytesOf(givenAlias))
                    .withNamespace(NamespaceSlot.Companion.at(currentNamespace)).fake(),
            PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf(givenAlias))
                .withNamespace(NamespaceSlot.Companion.at(targetNamespace)).fake()).fake();
            UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(inputOf(targetNamespace)).fake();

        // when
        inputHandler.handleInput( Input.Companion.inputOf(givenInput));

        // then
        then(userInterfaceAdapterPort).should(times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(1)).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().asString()
            .contains("Password entry with same alias already exists in target namespace");
        then(passwordService).should(never()).movePasswordEntry(any(Bytes.class), any(NamespaceSlot.class));
        assertThat(givenInput).isNotNull().isNotEqualTo(referenceInput);
    }

    private Input inputOf(final int index) {
        return  Input.Companion.inputOf(Bytes.bytesOf(String.valueOf(index)));
    }


}
