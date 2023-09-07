package de.pflugradts.passbird.application.boot.main;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.commandhandling.InputHandler;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.NamespaceServiceFake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.passbird.application.boot.main.PassbirdApplication.INTERRUPT;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._1;
import static de.pflugradts.passbird.domain.model.transfer.InputFakerKt.fakeInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PassbirdApplicationTest {

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Spy
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    @Mock
    private InputHandler inputHandler;
    @InjectMocks
    private PassbirdApplication passbirdApplication;

    @Captor
    private ArgumentCaptor<Output> captor;

    @Test
    void shouldDelegateInput() {
        // given
        final var input1 = fakeInput("1");
        final var input2 = fakeInput("2");
        final var input3 = fakeInput("3");
        final var interrupt = fakeInput(String.valueOf(INTERRUPT));

        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseInputs(input1, input2, input3, interrupt).fake();

        // when
        passbirdApplication.boot();

        // then
        then(inputHandler).should().handleInput(input1);
        then(inputHandler).should().handleInput(input2);
        then(inputHandler).should().handleInput(input3);
    }

    @Test
    void shouldDisplayNamespaceIfCurrentIsOtherThanDefault() {
        // given
        final var input1 = fakeInput("1");
        final var interrupt = fakeInput(String.valueOf(INTERRUPT));
        final var givenNamespace = "mynamespace";

        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(input1, interrupt).fake();
        namespaceServiceFake.deploy(Bytes.bytesOf(givenNamespace), _1);
        namespaceServiceFake.updateCurrentNamespace(_1);

        // when
        passbirdApplication.boot();

        // then
        then(userInterfaceAdapterPort).should(times(2)).receive(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().isEqualTo("[mynamespace] Enter command: ");
    }

    @Test
    void shouldDisplayNoNamespaceIfCurrentIsDefault() {
        // given
        final var input1 = fakeInput("1");
        final var interrupt = fakeInput(String.valueOf(INTERRUPT));

        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(input1, interrupt).fake();
        namespaceServiceFake.updateCurrentNamespace(DEFAULT);

        // when
        passbirdApplication.boot();

        // then
        then(userInterfaceAdapterPort).should(times(2)).receive(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().isEqualTo("Enter command: ");
    }

}
