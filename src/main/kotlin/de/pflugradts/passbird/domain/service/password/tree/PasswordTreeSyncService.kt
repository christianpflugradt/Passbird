package de.pflugradts.passbird.domain.service.password.tree

class PasswordTreeSyncService(private val eggRepositoryProvider: () -> EggRepository) {
    fun sync() = eggRepositoryProvider().sync()
}
