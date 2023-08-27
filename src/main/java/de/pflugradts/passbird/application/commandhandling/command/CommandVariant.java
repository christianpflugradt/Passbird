package de.pflugradts.passbird.application.commandhandling.command;

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
