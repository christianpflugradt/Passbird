package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import io.mockk.mockk
import io.mockk.spyk

fun createNestServiceForTesting() = NestingGroundService(mockk<EventRegistry>(relaxed = true))
fun createNestServiceSpyForTesting() = spyk(createNestServiceForTesting())
