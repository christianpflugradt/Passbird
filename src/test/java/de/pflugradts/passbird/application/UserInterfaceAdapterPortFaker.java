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
import static org.mockito.Mockito.lenient;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UserInterfaceAdapterPortFaker {

    private final AtomicInteger inputCount = new AtomicInteger(0);
    private final List<Input> inputList = new ArrayList<>();
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    public static UserInterfaceAdapterPortFaker faker() {
        return new UserInterfaceAdapterPortFaker();
    }

    public UserInterfaceAdapterPortFaker forInstance(final UserInterfaceAdapterPort userInterfaceAdapterPort) {
        this.userInterfaceAdapterPort = userInterfaceAdapterPort;
        return this;
    }

    public UserInterfaceAdapterPortFaker withTheseInputs(final Input... inputs) {
        inputList.addAll(Arrays.asList(inputs));
        return this;
    }

    private void givenReceivedInput() {
        lenient().when(userInterfaceAdapterPort.receive(any(Output.class))).thenAnswer(
                invocation -> inputList.get(inputCount.getAndIncrement()));
        lenient().when(userInterfaceAdapterPort.receive()).thenAnswer(
                invocation -> inputList.get(inputCount.getAndIncrement()));
    }

    public UserInterfaceAdapterPort fake() {
        givenReceivedInput();
        return userInterfaceAdapterPort;
    }

}
