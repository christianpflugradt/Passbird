package de.pflugradts.passbird.application.configuration

interface UpdatableConfiguration : ReadableConfiguration {
    fun updateDirectory(directory: String)
}
