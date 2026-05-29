package de.pflugradts.passbird.domain.service.password.tree

import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton

@Singleton
class PasswordTreeSyncService @Inject constructor(private val eggRepositoryProvider: Provider<EggRepository>) {
    fun sync() = eggRepositoryProvider.get().sync()
}
