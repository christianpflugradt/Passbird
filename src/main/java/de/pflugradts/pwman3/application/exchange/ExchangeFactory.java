package de.pflugradts.pwman3.application.exchange;

import de.pflugradts.pwman3.application.ExchangeAdapterPort;

public interface ExchangeFactory {
    ExchangeAdapterPort createPasswordExchange(String uri);
}
