package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import io.mockk.mockk
import io.mockk.spyk

fun createNestServiceForTesting() = NestingGroundService(mockk<EggRepository>(relaxed = true), mockk<EventRegistry>(relaxed = true))
fun createNestServiceSpyForTesting() = spyk(createNestServiceForTesting())
