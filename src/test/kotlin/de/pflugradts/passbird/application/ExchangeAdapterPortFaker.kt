package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.exchange.ExchangeFactory
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.Slot
import io.mockk.every
import io.mockk.mockk

fun fakeExchangeAdapterPort(
    forExchangeFactory: ExchangeFactory,
    withEggs: List<Egg> = emptyList(),
): ExchangeAdapterPort {
    val instance = mockk<ExchangeAdapterPort>()
    every { instance.send(any()) } returns Unit
    every { instance.receive() } answers {
        val result = mutableMapOf<Slot, MutableList<BytePair>>()
        withEggs.forEach {
            if (!result.containsKey(it.associatedNest())) { result[it.associatedNest()] = mutableListOf() }
            result[it.associatedNest()]!!.add(BytePair(Pair(it.viewKey(), it.viewPassword())))
        }
        result
    }
    every { forExchangeFactory.createPasswordExchange(any()) } returns instance
    return instance
}
