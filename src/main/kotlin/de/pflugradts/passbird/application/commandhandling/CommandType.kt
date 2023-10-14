package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.domain.model.transfer.Bytes

enum class CommandType(val type: Char) {
    CUSTOM_SET('c'),
    DISCARD('d'),
    EXPORT('e'),
    GET('g'),
    HELP('h'),
    IMPORT('i'),
    LIST('l'),
    NAMESPACE('n'),
    QUIT('q'),
    RENAME('r'),
    SET('s'),
    VIEW('v'),
    UNDEFINED('?'),
    ;

    companion object {
        fun resolveCommandTypeFrom(commandBytes: Bytes) = if (commandBytes.isNotEmpty) {
            entries.find { it.type.code.toByte() == commandBytes.firstByte } ?: UNDEFINED
        } else {
            UNDEFINED
        }
    }
}
