package de.pflugradts.pwman3.application.commandhandling.command;

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
    QUIT('q'),
    SET('s'),
    VIEW('v'),
    UNDEFINED('?');

    @Getter
    private final char type;

    CommandType(final char type) {
        this.type = type;
    }

    public static CommandType fromChar(final char type) {
        return Arrays.stream(CommandType.values())
                .filter(commandType -> commandType
                        .name()
                        .toLowerCase()
                        .charAt(0) == type)
                .findAny()
                .orElse(UNDEFINED);
    }

}
