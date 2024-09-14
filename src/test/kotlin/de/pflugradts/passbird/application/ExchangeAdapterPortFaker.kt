package de.pflugradts.passbird.application

import de.pflugradts.passbird.application.exchange.ExchangeFactory
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import io.mockk.every
import io.mockk.mockk

fun fakeExchangeAdapterPort(forExchangeFactory: ExchangeFactory, withEggs: List<Egg> = emptyList()): ExchangeAdapterPort {
    val instance = mockk<ExchangeAdapterPort>()
    every { instance.send(any()) } returns Unit
    every { instance.receive() } answers {
        val result = mutableMapOf<Nest, MutableList<PasswordInfo>>()
        withEggs.forEach {
            it.associatedNest().run {
                createNest(shellOf(this.name), this).run {
                    if (!result.containsKey(this)) result[this] = mutableListOf()
                    result[this]!!.add(
                        PasswordInfo(
                            first = ShellPair(it.viewEggId(), it.viewPassword()),
                            second = emptyList(),
                        ),
                    )
                }
            }
        }
        result
    }
    every { forExchangeFactory.createPasswordExchange() } returns instance
    return instance
}
