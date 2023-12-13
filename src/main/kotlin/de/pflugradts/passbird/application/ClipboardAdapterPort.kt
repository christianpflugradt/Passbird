package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Output

interface ClipboardAdapterPort {
    fun post(output: Output)
}
