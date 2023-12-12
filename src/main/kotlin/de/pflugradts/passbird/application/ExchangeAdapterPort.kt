package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.ShellPair

interface ExchangeAdapterPort {
    fun send(data: Map<NestSlot, List<ShellPair>>)
    fun receive(): Map<NestSlot, List<ShellPair>>
}
