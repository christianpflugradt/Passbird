package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.domain.model.shell.Shell

enum class CommandType(val type: Char) {
    CUSTOM_SET('c'),
    DISCARD('d'),
    EXPORT('e'),
    FAVORITE('f'),
    GET('g'),
    HELP('h'),
    IMPORT('i'),
    KEYSTORE('k'),
    LIST('l'),
    MEMORY('m'),
    NEST('n'),
    PROTEIN('p'),
    QUIT('q'),
    REPEAT('.'),
    RENAME('r'),
    SET('s'),
    VIEW('v'),
    YOLK('y'),
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
