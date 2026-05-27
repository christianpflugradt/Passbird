package de.pflugradts.passbird.application.util

import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.SECURE_RANDOM

internal inline fun <T> withScrambledBytes(bytes: ByteArray, block: (ByteArray) -> T): T = try {
    block(bytes)
} finally {
    SECURE_RANDOM.nextBytes(bytes)
}
