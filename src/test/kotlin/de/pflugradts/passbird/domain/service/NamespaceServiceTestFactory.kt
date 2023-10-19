package de.pflugradts.passbird.domain.service

import io.mockk.mockk
import io.mockk.spyk

fun createNamespaceServiceForTesting() = FixedNamespaceService(mockk(relaxed = true))
fun createNamespaceServiceSpyForTesting() = spyk(createNamespaceServiceForTesting())
