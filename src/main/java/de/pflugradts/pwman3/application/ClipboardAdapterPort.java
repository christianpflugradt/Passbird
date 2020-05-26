package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.domain.model.transfer.Output;

/**
 * AdapterPort for sending {@link Output} to the system clipboard.
 */
public interface ClipboardAdapterPort {
    void post(Output output);
}
