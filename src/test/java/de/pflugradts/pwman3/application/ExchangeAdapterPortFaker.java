package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.application.exchange.ExchangeFactory;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExchangeAdapterPortFaker {

    private ExchangeAdapterPort exchangeAdapterPort = mock(ExchangeAdapterPort.class);
    private List<PasswordEntry> passwordEntries = new ArrayList<>();
    private Throwable sendFailure;
    private Throwable receiveFailure;


    public static ExchangeAdapterPortFaker faker() {
        return new ExchangeAdapterPortFaker();
    }

    public ExchangeAdapterPortFaker forInstance(final ExchangeAdapterPort exchangeAdapterPort) {
        this.exchangeAdapterPort = exchangeAdapterPort;
        return this;
    }

    public ExchangeAdapterPortFaker usingFactory(final ExchangeFactory exchangeFactory) {
        given(exchangeFactory.createPasswordExchange(anyString())).willReturn(exchangeAdapterPort);
        return this;
    }

    public ExchangeAdapterPortFaker withPasswordEntries(final PasswordEntry... passwordEntries) {
        this.passwordEntries.clear();
        this.passwordEntries.addAll(Arrays.asList(passwordEntries));
        return this;
    }

    public ExchangeAdapterPortFaker withSendFailure(final Throwable sendFailure) {
        this.sendFailure = sendFailure;
        return this;
    }

    public ExchangeAdapterPortFaker withReceiveFailure(final Throwable receiveFailure) {
        this.receiveFailure = receiveFailure;
        return this;
    }

    public ExchangeAdapterPort fake() {
        given(exchangeAdapterPort.send(any())).willReturn(Objects.nonNull(sendFailure)
                ? Try.failure(sendFailure)
                : Try.success(null));

        given(exchangeAdapterPort.receive()).willReturn(Objects.nonNull(receiveFailure)
                ? Try.failure(receiveFailure)
                : Try.of(() -> passwordEntries
                        .stream()
                        .map(passwordEntry -> new Tuple2<>(
                                passwordEntry.viewKey(),
                                passwordEntry.viewPassword()))));
        return exchangeAdapterPort;
    }

}
