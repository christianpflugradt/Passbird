package de.pflugradts.passbird.domain.model.event

import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell

data class EggCreated(val egg: Egg) : DomainEvent
data class EggDiscarded(val egg: Egg) : DomainEvent
data class EggNotFound(val eggIdShell: Shell) : DomainEvent
data class EggRenamed(val egg: Egg) : DomainEvent
data class EggUpdated(val egg: Egg) : DomainEvent
data class EggsExported(val count: Int) : DomainEvent
data class EggsImported(val count: Int) : DomainEvent
data class NestCreated(val nest: Nest) : DomainEvent
data class NestDiscarded(val nest: Nest) : DomainEvent
