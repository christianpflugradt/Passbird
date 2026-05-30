package de.pflugradts.passbird.application.boot.migration

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.expectedGraphClasses
import de.pflugradts.passbird.application.boot.implementationClasses
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.migration.AuthenticatedMigrationDetector
import de.pflugradts.passbird.application.process.migration.Migration
import de.pflugradts.passbird.application.process.migration.MigrationRequest
import de.pflugradts.passbird.application.process.migration.PendingMigration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.domain.model.slot.Slot
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

@Tag(INTEGRATION)
class PassbirdMigrationGraphTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
        val migrationRequest = MigrationRequest(setOf(PendingMigration("keystore-format")))
        val actual = MigrationGraph(runContext, migrationRequest)

        // then
        expectThat(actual.bootable).isA<PassbirdMigration>()
        expectThat(actual.keyStoreAdapterPort).isA<KeyStoreAdapterPort>()
        expectThat(actual.userInterfaceAdapterPort).isA<UserInterfaceAdapterPort>()
        expectThat(actual.configuration).isA<ReadableConfiguration>()
        expectThat(actual.runContext) isSameInstanceAs runContext
        expectThat(actual.migrationRequest) isEqualTo migrationRequest
        expectThat(actual.authenticatedMigrationDetectors.implementationClasses()) isEqualTo expectedGraphClasses(
            AuthenticatedMigrationDetector::class.java,
        )
        expectThat(actual.migrations.implementationClasses()) isEqualTo expectedGraphClasses(Migration::class.java)
    }
}
