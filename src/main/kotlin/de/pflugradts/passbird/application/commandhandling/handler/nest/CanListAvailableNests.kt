package de.pflugradts.passbird.application.commandhandling.handler.nest

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.service.nest.NestService

abstract class CanListAvailableNests(
    private val nestService: NestService,
) {
    fun hasCustomNests() = nestService.all().anyMatch { it.isPresent }
    fun getAvailableNests(includeCurrent: Boolean) =
        (if (includeCurrent || nestService.currentNest() != DEFAULT) "\t0: ${DEFAULT.shell.asString()}\n" else "") +
            nestService.all()
                .filter { it.isPresent }
                .map { it.get() }
                .filter { includeCurrent || it != nestService.currentNest() }
                .map { "\t${it.nestSlot.index()}: ${it.shell.asString()}" }
                .toList().joinToString("\n")
}
