package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.Directory

interface UpdatableConfiguration : ReadableConfiguration {
    fun updateDirectory(directory: Directory)
}
