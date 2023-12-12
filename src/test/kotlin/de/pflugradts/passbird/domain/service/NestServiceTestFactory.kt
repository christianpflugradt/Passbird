package de.pflugradts.passbird.domain.service

import io.mockk.mockk
import io.mockk.spyk

fun createNestServiceForTesting() = NestingGroundService(mockk(relaxed = true))
fun createNestServiceSpyForTesting() = spyk(createNestServiceForTesting())
