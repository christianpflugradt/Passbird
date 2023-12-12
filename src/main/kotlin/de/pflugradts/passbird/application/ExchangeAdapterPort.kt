package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.shell.ShellPair

interface ExchangeAdapterPort {
    fun send(data: Map<Slot, List<ShellPair>>)
    fun receive(): Map<Slot, List<ShellPair>>
}
