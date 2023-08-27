package de.pflugradts.passbird.application;

import de.pflugradts.passbird.domain.model.transfer.Output;

/**
 * AdapterPort for sending {@link Output} to the system clipboard.
 */
public interface ClipboardAdapterPort {
    void post(Output output);
}
