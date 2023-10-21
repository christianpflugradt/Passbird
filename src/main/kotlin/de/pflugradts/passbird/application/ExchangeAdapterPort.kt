package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.BytePair
import java.util.stream.Stream

/**
 * AdapterPort for exchanging password data with a 3rd party.
 */
interface ExchangeAdapterPort {
    fun send(data: Stream<BytePair>)
    fun receive(): Stream<BytePair>
}
