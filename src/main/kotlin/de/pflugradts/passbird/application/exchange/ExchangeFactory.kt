package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.ExchangeAdapterPort

interface ExchangeFactory {
    fun createPasswordExchange(): ExchangeAdapterPort
}
