package de.pflugradts.passbird.application.commandhandling

enum class CommandVariant(val value: Char) {
    ADD('+'),
    DISCARD('-'),
    INFO('?'),
}
