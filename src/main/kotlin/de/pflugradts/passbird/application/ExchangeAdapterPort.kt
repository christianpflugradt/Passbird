package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot

/**
 * AdapterPort for exchanging password data with a 3rd party.
 */
interface ExchangeAdapterPort {
    fun send(data: Map<NamespaceSlot, List<BytePair>>)
    fun receive(): Map<NamespaceSlot, List<BytePair>>
}
