package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.nest.Slot

/**
 * AdapterPort for exchanging password data with a 3rd party.
 */
interface ExchangeAdapterPort {
    fun send(data: Map<Slot, List<BytePair>>)
    fun receive(): Map<Slot, List<BytePair>>
}
