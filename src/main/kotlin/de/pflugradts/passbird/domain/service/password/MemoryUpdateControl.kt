package de.pflugradts.passbird.domain.service.password

class MemoryUpdateControl {
    private var suppressionDepth = 0

    fun <T> withoutUpdates(block: () -> T): T {
        suppressionDepth += 1
        return try {
            block()
        } finally {
            suppressionDepth -= 1
        }
    }

    fun updatesEnabled() = suppressionDepth == 0
}
