package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.application.ExchangeAdapterPort

fun interface ExchangeFactory {
    fun createPasswordExchange(): ExchangeAdapterPort
}
