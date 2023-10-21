package de.pflugradts.passbird.domain.model

import de.pflugradts.passbird.domain.model.transfer.Bytes

@JvmInline
value class BytePair(val value: Pair<Bytes, Bytes>)
