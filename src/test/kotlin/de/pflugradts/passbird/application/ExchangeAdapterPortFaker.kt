package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.exchange.ExchangeFactory
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every
import io.mockk.mockk

fun fakeExchangeAdapterPort(
    forExchangeFactory: ExchangeFactory,
    withEggs: List<Egg> = emptyList(),
): ExchangeAdapterPort {
    val instance = mockk<ExchangeAdapterPort>()
    every { instance.send(any()) } returns Unit
    every { instance.receive() } answers {
        val result = mutableMapOf<Slot, MutableList<ShellPair>>()
        withEggs.forEach {
            if (!result.containsKey(it.associatedNest())) { result[it.associatedNest()] = mutableListOf() }
            result[it.associatedNest()]!!.add(ShellPair(it.viewEggId(), it.viewPassword()))
        }
        result
    }
    every { forExchangeFactory.createPasswordExchange() } returns instance
    return instance
}
