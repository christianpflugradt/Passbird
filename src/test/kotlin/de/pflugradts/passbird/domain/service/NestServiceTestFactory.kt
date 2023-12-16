package de.pflugradts.passbird.domain.service

import io.mockk.spyk

fun createNestServiceForTesting() = NestingGroundService()
fun createNestServiceSpyForTesting() = spyk(createNestServiceForTesting())
