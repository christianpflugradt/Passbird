package de.pflugradts.passbird.application.commandhandling.handler.nest

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.service.NestService

abstract class CanListAvailableNests(
    private val nestService: NestService,
) {
    fun hasCustomNests() = nestService.all().anyMatch { it.isPresent }
    fun getAvailableNests(includeCurrent: Boolean) =
        (if (includeCurrent || nestService.getCurrentNest() != DEFAULT) "\t0: ${DEFAULT.bytes.asString()}\n" else "") +
            nestService.all()
                .filter { it.isPresent }
                .map { it.get() }
                .filter { includeCurrent || it != nestService.getCurrentNest() }
                .map { "\t${it.slot.index()}: ${it.bytes.asString()}" }
                .toList().joinToString("\n")
}
