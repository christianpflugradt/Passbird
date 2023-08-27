package de.pflugradts.passbird.application.boot;

import de.pflugradts.passbird.application.util.SystemOperation;

public interface Bootable {
    void boot();
    default void terminate(final SystemOperation systemOperation) {
        systemOperation.exit();
    }
}
