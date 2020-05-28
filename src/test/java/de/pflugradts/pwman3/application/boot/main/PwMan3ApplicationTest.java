package de.pflugradts.pwman3.application.boot.main;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.pwman3.application.commandhandling.InputHandler;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.InputFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static de.pflugradts.pwman3.application.boot.main.PwMan3Application.INTERRUPT;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PwMan3ApplicationTest {

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private InputHandler inputHandler;
    @InjectMocks
    private PwMan3Application pwMan3Application;

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

}
