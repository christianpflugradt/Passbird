package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.nest.Slot

interface ExchangeAdapterPort {
    fun send(data: Map<Slot, List<BytePair>>)
    fun receive(): Map<Slot, List<BytePair>>
}
