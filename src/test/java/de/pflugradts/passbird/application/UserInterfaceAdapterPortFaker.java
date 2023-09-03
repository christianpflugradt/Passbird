package de.pflugradts.passbird.application;

import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UserInterfaceAdapterPortFaker {

    private final AtomicInteger inputCount = new AtomicInteger(0);
    private final AtomicInteger secureInputCount = new AtomicInteger(0);
    private final List<Input> inputList = new ArrayList<>();
    private final List<Input> secureInputList = new ArrayList<>();
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    public static UserInterfaceAdapterPortFaker faker() {
        return new UserInterfaceAdapterPortFaker();
    }

    public UserInterfaceAdapterPortFaker forInstance(final UserInterfaceAdapterPort userInterfaceAdapterPort) {
        this.userInterfaceAdapterPort = userInterfaceAdapterPort;
        return this;
    }

    public UserInterfaceAdapterPortFaker withReceiveConfirmation(final boolean confirmed) {
        given(userInterfaceAdapterPort.receiveConfirmation(any(Output.class))).willReturn(confirmed);
        return this;
    }

    public UserInterfaceAdapterPortFaker withTheseInputs(final Input... inputs) {
        inputList.addAll(Arrays.asList(inputs));
        return this;
    }

    public UserInterfaceAdapterPortFaker withTheseSecureInputs(final Input... inputs) {
        secureInputList.addAll(Arrays.asList(inputs));
        return this;
    }

    private void givenReceivedInput() {
        lenient().when(userInterfaceAdapterPort.receive(any(Output.class))).thenAnswer(
                invocation -> inputList.get(inputCount.getAndIncrement()));
        lenient().when(userInterfaceAdapterPort.receive()).thenAnswer(
                invocation -> inputList.get(inputCount.getAndIncrement()));
    }

    private void givenReceivedSecureInput() {
        lenient().when(userInterfaceAdapterPort.receiveSecurely(any(Output.class))).thenAnswer(
                invocation -> secureInputList.get(secureInputCount.getAndIncrement()));
        lenient().when(userInterfaceAdapterPort.receiveSecurely()).thenAnswer(
                invocation -> secureInputList.get(secureInputCount.getAndIncrement()));
    }

    public UserInterfaceAdapterPort fake() {
        givenReceivedInput();
        givenReceivedSecureInput();
        return userInterfaceAdapterPort;
    }

}
