package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.Tuple
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.stream.Stream

/**
 * AdapterPort for exchanging password data with a 3rd party.
 */
interface ExchangeAdapterPort {
    fun send_Deprecated(data: Stream<Tuple<Bytes, Bytes>>)
    fun send(data: Stream<BytePair>)
    fun receive(): Stream<BytePair>
}

@JvmInline
value class BytePair(val value: Pair<Bytes, Bytes>)
