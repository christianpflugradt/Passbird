package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.Directory

interface ConfigurationSync { fun sync(directory: Directory) }
