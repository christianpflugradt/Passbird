package de.pflugradts.pwman3.application.commandhandling.command;

import lombok.Getter;

public enum CommandVariant {

    ADD('+'),
    DISCARD('-');

    @Getter
    private char value;

    CommandVariant(final char value) {
        this.value = value;
    }

}
