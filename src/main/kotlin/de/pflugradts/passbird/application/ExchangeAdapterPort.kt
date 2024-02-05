package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot

interface ExchangeAdapterPort {
    fun send(data: ShellPairMap)
    fun receive(): ShellPairMap
}

typealias ShellPairMap = Map<Slot, List<ShellPair>>
