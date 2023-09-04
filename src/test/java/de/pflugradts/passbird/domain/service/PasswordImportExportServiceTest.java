package de.pflugradts.passbird.domain.service;

import de.pflugradts.passbird.application.ExchangeAdapterPort;
import de.pflugradts.passbird.application.ExchangeAdapterPortFaker;
import de.pflugradts.passbird.application.exchange.ExchangeFactory;
import de.pflugradts.passbird.application.exchange.PasswordImportExportService;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import io.vavr.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordImportExportServiceTest {

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private ExchangeFactory exchangeFactory;
    @Mock
    private PasswordService passwordService;
    @InjectMocks
    private PasswordImportExportService importExportService;

    @Captor
    private ArgumentCaptor<Stream<Tuple2<Bytes, Bytes>>> captor;

    private final String URI = "any uri";

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldPeekImportKeyBytes() {
        // given
        final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key1"))
                .withPasswordBytes(Bytes.bytesOf("password1")).fake();
        final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key2"))
                .withPasswordBytes(Bytes.bytesOf("password2")).fake();
        final var exchangeAdapterPort = mock(ExchangeAdapterPort.class);
        ExchangeAdapterPortFaker.faker()
                .forInstance(exchangeAdapterPort)
                .usingFactory(exchangeFactory)
                .withPasswordEntries(passwordEntry1, passwordEntry2).fake();

        // when
        final var actual = importExportService.peekImportKeyBytes(URI);

        // then
        then(exchangeFactory).should().createPasswordExchange(URI);
        then(passwordService).shouldHaveNoInteractions();
        assertThat(actual).containsExactlyInAnyOrder(passwordEntry1.viewKey(), passwordEntry2.viewKey());
    }

    @Test
    void shouldImportPasswords() {
        // given
        final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key1"))
                .withPasswordBytes(Bytes.bytesOf("password1")).fake();
        final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key2"))
                .withPasswordBytes(Bytes.bytesOf("password2")).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService).fake();
        final var exchangeAdapterPort = mock(ExchangeAdapterPort.class);
        ExchangeAdapterPortFaker.faker()
                .forInstance(exchangeAdapterPort)
                .usingFactory(exchangeFactory)
                .withPasswordEntries(passwordEntry1, passwordEntry2).fake();

        // when
        importExportService.importPasswordEntries(URI);

        // then
        then(exchangeFactory).should().createPasswordExchange(URI);
        then(passwordService).should().putPasswordEntries(captor.capture());
        assertThat(captor.getValue().collect(Collectors.toList()))
                .containsExactly(
                        new Tuple2<>(passwordEntry1.viewKey(), passwordEntry1.viewPassword()),
                        new Tuple2<>(passwordEntry2.viewKey(), passwordEntry2.viewPassword()));
    }

    @Test
    void shouldHandleImportFailure() {
        // given
        final Throwable failure = mock(Throwable.class);
        ExchangeAdapterPortFaker.faker()
                .usingFactory(exchangeFactory)
                .withReceiveFailure(failure).fake();

        // when
        importExportService.importPasswordEntries(URI);

        // then
        then(failureCollector).should().collectImportFailure(failure);
        then(passwordService).shouldHaveNoInteractions();
    }

    @Test
    void shouldExportPasswords() {
        // given
        final var passwordEntry1 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key1"))
                .withPasswordBytes(Bytes.bytesOf("password1")).fake();
        final var passwordEntry2 = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("key2"))
                .withPasswordBytes(Bytes.bytesOf("password2")).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(passwordEntry1, passwordEntry2).fake();
        final var exchangeAdapterPort = mock(ExchangeAdapterPort.class);
        ExchangeAdapterPortFaker.faker()
                .forInstance(exchangeAdapterPort)
                .usingFactory(exchangeFactory).fake();

        // when
        importExportService.exportPasswordEntries(URI);

        // then
        then(exchangeFactory).should().createPasswordExchange(URI);
        then(exchangeAdapterPort).should().send(captor.capture());
        assertThat(captor.getValue().collect(Collectors.toList()))
                .containsExactly(
                        new Tuple2<>(passwordEntry1.viewKey(), passwordEntry1.viewPassword()),
                        new Tuple2<>(passwordEntry2.viewKey(), passwordEntry2.viewPassword()));
    }

    @Test
    void shouldHandleExportFailure() {
        // given
        final Throwable failure = mock(Throwable.class);
        ExchangeAdapterPortFaker.faker()
                .usingFactory(exchangeFactory)
                .withSendFailure(failure).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(PasswordEntryFaker.faker().fakePasswordEntry().fake()).fake();

        // when
        importExportService.exportPasswordEntries(URI);

        // then
        then(failureCollector).should().collectExportFailure(failure);
    }

}
