package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.domain.model.transfer.Output;

public interface ClipboardAdapterPort {
    void post(Output output);
}
