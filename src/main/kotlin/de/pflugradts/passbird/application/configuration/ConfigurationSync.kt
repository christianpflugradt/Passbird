package de.pflugradts.passbird.application.configuration

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.application.Directory

interface ConfigurationSync {
    fun sync(directory: Directory): TryResult<Unit>
    fun syncKeyStoreLocation(configurationDirectory: Directory, keyStoreDirectory: Directory): TryResult<Unit>
}
