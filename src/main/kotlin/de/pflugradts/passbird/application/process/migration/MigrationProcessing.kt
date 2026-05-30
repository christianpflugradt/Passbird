package de.pflugradts.passbird.application.process.migration
data class PendingMigration(val id: String)
data class MigrationRequest(
    val pendingMigrations: Set<PendingMigration>,
) {
    val required get() = pendingMigrations.isNotEmpty()
    operator fun plus(other: MigrationRequest) = MigrationRequest(pendingMigrations + other.pendingMigrations)
    companion object {
        fun empty() = MigrationRequest(emptySet())
    }
}
interface PreLaunchMigrationDetector {
    fun detect(): MigrationRequest
}
interface AuthenticatedMigrationDetector {
    fun detect(): MigrationRequest
}
interface Migration {
    val id: String
    val order: Int
    fun run()
}
class PreLaunchMigrationLocator constructor(
    private val detectors: Set<PreLaunchMigrationDetector>,
) {
    fun detect() = detectors.fold(MigrationRequest.empty()) { request, detector -> request + detector.detect() }
}
class AuthenticatedMigrationLocator constructor(
    private val detectors: Set<AuthenticatedMigrationDetector>,
) {
    fun detect() = detectors.fold(MigrationRequest.empty()) { request, detector -> request + detector.detect() }
}
class MigrationRunner constructor(
    private val migrations: Set<Migration>,
) {
    fun run(request: MigrationRequest) {
        val registeredMigrations = migrations.associateBy(Migration::id)
        check(registeredMigrations.size == migrations.size)
        request.pendingMigrations
            .map { pendingMigration ->
                registeredMigrations[pendingMigration.id]
                    ?: throw IllegalStateException("No migration registered for '${pendingMigration.id}'.")
            }
            .sortedBy(Migration::order)
            .forEach(Migration::run)
    }
}
