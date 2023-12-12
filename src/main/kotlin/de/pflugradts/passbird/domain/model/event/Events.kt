package de.pflugradts.passbird.domain.model.event

import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.transfer.Bytes

data class EggCreated(val egg: Egg) : DomainEvent
data class EggDiscarded(val egg: Egg) : DomainEvent
data class EggNotFound(val eggIdBytes: Bytes) : DomainEvent
data class EggRenamed(val egg: Egg) : DomainEvent
data class EggUpdated(val egg: Egg) : DomainEvent
