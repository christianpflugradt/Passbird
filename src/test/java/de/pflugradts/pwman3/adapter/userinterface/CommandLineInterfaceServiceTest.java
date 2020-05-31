package de.pflugradts.pwman3.adapter.userinterface;

import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.application.util.SystemOperationFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommandLineInterfaceServiceTest {

    @Mock
    private SystemOperation systemOperation;
    @Mock
    private Configuration configuration;
    @InjectMocks
    private CommandLineInterfaceService commandLineInterfaceService;

    @Nested
    class SendTest {

        @Test
        void shouldSendOutput() throws IOException {
            // given
            final var givenMessage = "hello world";
            final var expectedMessage = givenMessage + System.lineSeparator();
            String actual;
            try (var stream = new ByteArrayOutputStream(); var printStream = new PrintStream(stream)) {
                System.setOut(printStream);
                ConfigurationFaker.faker().forInstance(configuration);

                // when
                commandLineInterfaceService.send(Output.of(Bytes.of(givenMessage)));

                // then
                actual = new String(stream.toByteArray());
            }
            assertThat(actual).isNotNull().isEqualTo(expectedMessage);
        }

        @Test
        void shouldSendLineBreak() throws IOException {
            // given
            final var expectedMessage = System.lineSeparator();
            String actual;
            try (var stream = new ByteArrayOutputStream(); var printStream = new PrintStream(stream)) {
                System.setOut(printStream);

                // when
                commandLineInterfaceService.sendLineBreak();

                // then
                actual = new String(stream.toByteArray());
            }
            assertThat(actual).isNotNull().isEqualTo(expectedMessage);
        }

    }

    @Nested
    class ReceiveTest {

        @Test
        void shouldReceiveInput() throws IOException {
            // given
            final var givenInput = "hello world";
            final var inputBytes = (givenInput + System.lineSeparator()).getBytes();
            Try<Input> actual;
            try (var stream = new ByteArrayInputStream(inputBytes)) {
                System.setIn(stream);
                ConfigurationFaker.faker().forInstance(configuration);

                // when
                actual = commandLineInterfaceService.receive();
            }

            // then
            assertThat(actual.isSuccess()).isTrue();
            assertThat(actual.get()).isNotNull()
                    .extracting(Input::getBytes).isNotNull()
                    .extracting(Bytes::asString).isNotNull()
                    .isEqualTo(givenInput);
        }

        @Test
        void shouldReceiveInput_SendOutput() throws IOException {
            // given
            final var givenMessage = "hello world";
            String actual;
            try (var stream = new ByteArrayOutputStream();
                 var printStream = new PrintStream(stream);
                 var in = new ByteArrayInputStream(("smth" + System.lineSeparator()).getBytes())) {
                System.setOut(printStream);
                System.setIn(in);
                ConfigurationFaker.faker().forInstance(configuration);

                // when
                commandLineInterfaceService.receive(Output.of(Bytes.of(givenMessage)));

                // then
                actual = new String(stream.toByteArray());
            }
            assertThat(actual).isNotNull().isEqualTo(givenMessage);
        }

    }

    @Nested
    class ReceiveSecurelyTest {

        @Test
        void shouldReceiveInputSecurely() {
            // given
            final var givenInput = "hello world";
            SystemOperationFaker.faker()
                    .forInstance(systemOperation)
                    .withPasswordFromConsole(givenInput.toCharArray()).fake();
            ConfigurationFaker.faker()
                    .forInstance(configuration)
                    .withSecureInputEnabled().fake();

            // when
            final var actual = commandLineInterfaceService.receiveSecurely();

            // then
            assertThat(actual.isSuccess()).isTrue();
            assertThat(actual.get()).isNotNull()
                    .extracting(Input::getBytes).isNotNull()
                    .extracting(Bytes::asString).isNotNull()
                    .isEqualTo(givenInput);
        }

        @Test
        void shouldReceiveInputSecurely_SendOutput() throws IOException {
            // given
            final var givenMessage = "hello world";
            String actual;
            try (var stream = new ByteArrayOutputStream();
                 var printStream = new PrintStream(stream)) {
                System.setOut(printStream);
                SystemOperationFaker.faker()
                        .forInstance(systemOperation)
                        .withPasswordFromConsole("smth".toCharArray()).fake();
                ConfigurationFaker.faker()
                        .forInstance(configuration)
                        .withSecureInputEnabled().fake();

                // when
                commandLineInterfaceService.receiveSecurely(Output.of(Bytes.of(givenMessage)));

                // then
                actual = new String(stream.toByteArray());
            }
            assertThat(actual).isNotNull().isEqualTo(givenMessage);
        }

        @Test
        void shouldReceiveInputSecurely_ReceivePlainIfSecureInputIsDisabled() throws IOException {
            // given
            final var givenInput = "hello world";
            final var inputBytes = (givenInput + System.lineSeparator()).getBytes();
            Try<Input> actual;
            try (var stream = new ByteArrayInputStream(inputBytes)) {
                System.setIn(stream);
                ConfigurationFaker.faker()
                        .forInstance(configuration)
                        .withSecureInputDisabled().fake();

                // when
                actual = commandLineInterfaceService.receiveSecurely();
            }

            // then
            assertThat(actual.isSuccess()).isTrue();
            assertThat(actual.get()).isNotNull()
                    .extracting(Input::getBytes).isNotNull()
                    .extracting(Bytes::asString).isNotNull()
                    .isEqualTo(givenInput);
        }

        @Test
        void shouldReceiveInputSecurely_ReceivePlainIfConsoleIsUnavailable() throws IOException {
            // given
            final var givenInput = "hello world";
            final var inputBytes = (givenInput + System.lineSeparator()).getBytes();
            Try<Input> actual;
            try (var stream = new ByteArrayInputStream(inputBytes)) {
                System.setIn(stream);
                SystemOperationFaker.faker()
                        .forInstance(systemOperation)
                        .withConsoleDisabled().fake();
                ConfigurationFaker.faker()
                        .forInstance(configuration)
                        .withSecureInputEnabled().fake();

                // when
                actual = commandLineInterfaceService.receiveSecurely();
            }

            // then
            assertThat(actual.isSuccess()).isTrue();
            assertThat(actual.get()).isNotNull()
                    .extracting(Input::getBytes).isNotNull()
                    .extracting(Bytes::asString).isNotNull()
                    .isEqualTo(givenInput);
        }

    }

    @Nested
    class ReceiveConfirmationTest {

        @Test
        void shouldReceiveConfirmation_c_ReturnsTrue() throws IOException {
            // given
            final var givenInput = "c";
            final var inputBytes = (givenInput + System.lineSeparator()).getBytes();
            boolean actual;
            try (var stream = new ByteArrayInputStream(inputBytes)) {
                System.setIn(stream);
                ConfigurationFaker.faker().forInstance(configuration);

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty());
            }

            // then
            assertThat(actual).isTrue();
        }

        @Test
        void shouldReceiveConfirmation_cc_ReturnsFalse() throws IOException {
            // given
            final var givenInput = "cc";
            final var inputBytes = (givenInput + System.lineSeparator()).getBytes();
            boolean actual;
            try (var stream = new ByteArrayInputStream(inputBytes)) {
                System.setIn(stream);
                ConfigurationFaker.faker().forInstance(configuration);

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty());
            }

            // then
            assertThat(actual).isFalse();
        }

        @Test
        void shouldReceiveConfirmation_d_ReturnsFalse() throws IOException {
            // given
            final var givenInput = "d";
            final var inputBytes = (givenInput + System.lineSeparator()).getBytes();
            boolean actual;
            try (var stream = new ByteArrayInputStream(inputBytes)) {
                System.setIn(stream);
                ConfigurationFaker.faker().forInstance(configuration);

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty());
            }

            // then
            assertThat(actual).isFalse();
        }

        @Test
        void shouldReceiveConfirmation_empty_ReturnsFalse() throws IOException {
            // given
            final var givenInput = "";
            final var inputBytes = (givenInput + System.lineSeparator()).getBytes();
            boolean actual;
            try (var stream = new ByteArrayInputStream(inputBytes)) {
                System.setIn(stream);
                ConfigurationFaker.faker().forInstance(configuration);

                // when
                actual = commandLineInterfaceService.receiveConfirmation(Output.empty());
            }

            // then
            assertThat(actual).isFalse();
        }

    }

}
