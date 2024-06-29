package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.ShellPair

interface ExchangeAdapterPort {
    fun send(data: ShellPairMap)
    fun receive(): ShellPairMap
}

typealias ShellPairMap = Map<Nest, List<ShellPair>>
