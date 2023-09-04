package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ListCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private PasswordService passwordService;
    @InjectMocks
    private ListCommandHandler listCommandHandler;

    @Captor
    private ArgumentCaptor<Output> captor;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(listCommandHandler);
    }

    @Test
    void shouldHandleListCommand() {
        // given
        final var input = Input.of(Bytes.bytesOf("l"));
        final var key1 = Bytes.bytesOf("key1");
        final var key2 = Bytes.bytesOf("key2");
        final var key3 = Bytes.bytesOf("key3");
        given(passwordService.findAllKeys()).willReturn(Try.of(() -> Stream.of(key1, key2, key3)));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull().asString()
                .contains(key1.asString())
                .contains(key2.asString())
                .contains(key3.asString());
    }

    @Test
    void shouldHandleListCommand_WithEmptyDatabase() {
        // given
        final var input = Input.of(Bytes.bytesOf("l"));
        given(passwordService.findAllKeys()).willReturn(Try.of(Stream::empty));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue()).isNotNull()
                .extracting(Output::getBytes).isNotNull()
                .extracting(Bytes::asString).isNotNull().asString()
                .isEqualTo("database is empty");
    }


}
