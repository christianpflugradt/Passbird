package de.pflugradts.passbird.application;

import de.pflugradts.passbird.application.exchange.ExchangeFactory;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
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
        doNothing().when(exchangeAdapterPort).send(any());

        given(exchangeAdapterPort.receive()).willReturn(Objects.nonNull(receiveFailure)
                ? Stream.empty()
                : passwordEntries
                        .stream()
                        .map(passwordEntry -> new Tuple<>(
                                passwordEntry.viewKey(),
                                passwordEntry.viewPassword())));
        return exchangeAdapterPort;
    }

}
