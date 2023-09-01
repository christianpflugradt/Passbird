package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Output

/**
 * AdapterPort for sending [Output] to the system clipboard.
 */
interface ClipboardAdapterPort {
    fun post(output: Output)
}
