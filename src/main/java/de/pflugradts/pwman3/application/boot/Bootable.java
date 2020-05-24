package de.pflugradts.pwman3.application.boot;

import de.pflugradts.pwman3.application.util.SystemOperation;

public interface Bootable {
    void boot();
    default void terminate(final SystemOperation systemOperation) {
        systemOperation.exit();
    }
}
