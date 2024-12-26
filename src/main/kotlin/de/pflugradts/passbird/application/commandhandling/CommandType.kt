package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.domain.model.shell.Shell

enum class CommandType(val type: Char) {
    CUSTOM_SET('c'),
    DISCARD('d'),
    EXPORT('e'),
    GET('g'),
    HELP('h'),
    IMPORT('i'),
    LIST('l'),
    MEMORY('m'),
    NEST('n'),
    PROTEIN('p'),
    QUIT('q'),
    RENAME('r'),
    SET('s'),
    VIEW('v'),
    UNDEFINED('?'),
    ;

    companion object {
        fun resolveCommandTypeFrom(commandShell: Shell) = if (commandShell.isNotEmpty) {
            entries.find { it.type.code.toByte() == commandShell.firstByte } ?: UNDEFINED
        } else {
            UNDEFINED
        }
    }
}
