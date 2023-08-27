package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.Getter;

import java.util.Arrays;

public enum CommandType {

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
    UNDEFINED('?');

    @Getter
    private final char type;

    CommandType(final char type) {
        this.type = type;
    }

    public static CommandType fromCommandBytes(final Bytes type) {
        return type.isEmpty() ? UNDEFINED : Arrays.stream(CommandType.values())
            .filter(commandType -> commandType
                    .name()
                    .toLowerCase()
                    .charAt(0) == type.getFirstByte())
            .findAny()
            .orElse(UNDEFINED);
    }

}
