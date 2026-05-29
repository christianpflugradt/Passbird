package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.shell.Shell

inline fun <T> Shell.useScrambled(block: (Shell) -> T): T = try {
    block(this)
} finally {
    scramble()
}
