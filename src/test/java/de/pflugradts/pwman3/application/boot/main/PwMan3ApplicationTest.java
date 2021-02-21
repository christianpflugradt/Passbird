package de.pflugradts.pwman3.application.boot.main;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.pwman3.application.commandhandling.InputHandler;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.InputFaker;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.NamespaceServiceFake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.pwman3.application.boot.main.PwMan3Application.INTERRUPT;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.DEFAULT;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot._1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PwMan3ApplicationTest {

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Spy
    private final NamespaceServiceFake namespaceServiceFake = new NamespaceServiceFake();
    @Mock
    private InputHandler inputHandler;
    @InjectMocks
    private PwMan3Application pwMan3Application;

    @Captor
    private ArgumentCaptor<Output> captor;

    @Test
    void shouldDelegateInput() {
        // given
        final var input1 = InputFaker.faker().fakeInput().withMessage("1").fake();
        final var input2 = InputFaker.faker().fakeInput().withMessage("2").fake();
        final var input3 = InputFaker.faker().fakeInput().withMessage("3").fake();
        final var interrupt = InputFaker.faker().fakeInput().withMessage(String.valueOf(INTERRUPT)).fake();

        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseInputs(input1, input2, input3, interrupt).fake();

        // when
        pwMan3Application.boot();

        // then
        then(inputHandler).should().handleInput(input1);
        then(inputHandler).should().handleInput(input2);
        then(inputHandler).should().handleInput(input3);
    }

    @Test
    void shouldDisplayNamespaceIfCurrentIsOtherThanDefault() {
        // given
        final var input1 = InputFaker.faker().fakeInput().withMessage("1").fake();
        final var interrupt = InputFaker.faker().fakeInput().withMessage(String.valueOf(INTERRUPT)).fake();
        final var givenNamespace = "mynamespace";

        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(input1, interrupt).fake();
        namespaceServiceFake.deploy(Bytes.of(givenNamespace), _1);
        namespaceServiceFake.updateCurrentNamespace(_1);

        // when
        pwMan3Application.boot();

        // then
        then(userInterfaceAdapterPort).should(times(2)).receive(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().isEqualTo("[mynamespace] Enter command: ");
    }

    @Test
    void shouldDisplayNoNamespaceIfCurrentIsDefault() {
        // given
        final var input1 = InputFaker.faker().fakeInput().withMessage("1").fake();
        final var interrupt = InputFaker.faker().fakeInput().withMessage(String.valueOf(INTERRUPT)).fake();

        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort)
            .withTheseInputs(input1, interrupt).fake();
        namespaceServiceFake.updateCurrentNamespace(DEFAULT);

        // when
        pwMan3Application.boot();

        // then
        then(userInterfaceAdapterPort).should(times(2)).receive(captor.capture());
        assertThat(captor.getValue()).isNotNull()
            .extracting(Output::getBytes).isNotNull()
            .extracting(Bytes::asString).isNotNull().isEqualTo("Enter command: ");
    }

}
