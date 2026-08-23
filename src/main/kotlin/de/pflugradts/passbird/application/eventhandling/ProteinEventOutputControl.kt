package de.pflugradts.passbird.application.eventhandling

class ProteinEventOutputControl {
    private var suppressions = 0

    fun suppress(block: () -> Unit) {
        suppressions++
        try {
            block()
        } finally {
            suppressions--
        }
    }

    fun proteinEventsSuppressed() = suppressions > 0
}
