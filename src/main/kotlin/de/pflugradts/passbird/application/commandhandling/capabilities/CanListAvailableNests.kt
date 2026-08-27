package de.pflugradts.passbird.application.commandhandling.capabilities

import de.pflugradts.passbird.application.commandhandling.handler.labeledValueLine
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.service.nest.NestService

class CanListAvailableNests constructor(private val nestService: NestService) {
    fun hasCustomNests() = nestService.all().anyMatch { it.isPresent }
    fun getAvailableNests(includeCurrent: Boolean) = (
        if (includeCurrent ||
            nestService.currentNest() != DEFAULT
        ) {
            "${labeledValueLine("0:", DEFAULT.viewNestId().asString())}\n"
        } else {
            ""
        }
        ) +
        nestService.all()
            .filter { it.isPresent }
            .map { it.get() }
            .filter { includeCurrent || it != nestService.currentNest() }
            .map { labeledValueLine("${it.slot.index()}:", it.viewNestId().asString()) }
            .toList().joinToString("\n")
}
