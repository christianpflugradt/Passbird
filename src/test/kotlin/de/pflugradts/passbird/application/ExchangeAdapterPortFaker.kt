package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.exchange.ExchangeFactory
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import io.mockk.every
import io.mockk.mockk

fun fakeExchangeAdapterPort(
    forExchangeFactory: ExchangeFactory,
    withPasswordEntries: List<PasswordEntry> = emptyList(),
): ExchangeAdapterPort {
    val instance = mockk<ExchangeAdapterPort>()
    every { instance.send(any()) } returns Unit
    every { instance.receive() } returns withPasswordEntries.map { BytePair(Pair(it.viewKey(), it.viewPassword())) }.stream()
    every { forExchangeFactory.createPasswordExchange(any()) } returns instance
    return instance
}
