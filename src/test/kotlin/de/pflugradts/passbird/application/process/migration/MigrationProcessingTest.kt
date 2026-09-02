package de.pflugradts.passbird.application.process.migration

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MigrationProcessingTest {

    @Test
    fun `should combine pre-launch migration requests`() {
        val locator = PreLaunchMigrationLocator(
            setOf(
                object : PreLaunchMigrationDetector {
                    override fun detect() = MigrationRequest(setOf(PendingMigration("first")))
                },
                object : PreLaunchMigrationDetector {
                    override fun detect() = MigrationRequest(setOf(PendingMigration("second")))
                },
            ),
        )

        expectThat(locator.detect()) isEqualTo MigrationRequest(
            setOf(
                PendingMigration("first"),
                PendingMigration("second"),
            ),
        )
    }

    @Test
    fun `should combine authenticated migration requests`() {
        val locator = AuthenticatedMigrationLocator(
            setOf(
                object : AuthenticatedMigrationDetector {
                    override fun detect() = MigrationRequest(setOf(PendingMigration("first")))
                },
                object : AuthenticatedMigrationDetector {
                    override fun detect() = MigrationRequest(setOf(PendingMigration("second")))
                },
            ),
        )

        expectThat(locator.detect()) isEqualTo MigrationRequest(
            setOf(
                PendingMigration("first"),
                PendingMigration("second"),
            ),
        )
    }

    @Test
    fun `should mark only non-empty migration requests as required`() {
        expectThat(MigrationRequest.empty().required).isEqualTo(false)
        expectThat(MigrationRequest(setOf(PendingMigration("migration"))).required).isEqualTo(true)
    }
}
