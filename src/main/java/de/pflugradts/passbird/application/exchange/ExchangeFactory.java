package de.pflugradts.passbird.application.exchange;

import de.pflugradts.passbird.application.ExchangeAdapterPort;

public interface ExchangeFactory {
    ExchangeAdapterPort createPasswordExchange(String uri);
}
