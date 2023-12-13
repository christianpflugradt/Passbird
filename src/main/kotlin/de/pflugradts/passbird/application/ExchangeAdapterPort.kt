package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.ShellPair

interface ExchangeAdapterPort {
    fun send(data: ShellPairMap)
    fun receive(): ShellPairMap
}

typealias ShellPairMap = Map<NestSlot, List<ShellPair>>
