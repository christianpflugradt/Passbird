package de.pflugradts.passbird.application.process.migration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MigrationRunnerTest {

    @Test
    fun `should run pending migrations in order`() {
        // given
        val calls = mutableListOf<String>()
        val migrationRunner = MigrationRunner(
            setOf(
                testMigration(id = "password-tree-format", order = 2, calls = calls),
                testMigration(id = "keystore-format", order = 1, calls = calls),
            ),
        )

        // when
        migrationRunner.run(
            MigrationRequest(
                setOf(
                    PendingMigration("password-tree-format"),
                    PendingMigration("keystore-format"),
                ),
            ),
        )

        // then
        expectThat(calls.toList()) isEqualTo listOf("keystore-format", "password-tree-format")
    }

    @Test
    fun `should fail if a pending migration has no registered migration`() {
        // given
        val migrationRunner = MigrationRunner(emptySet())

        // when / then
        assertThrows<IllegalStateException> {
            migrationRunner.run(MigrationRequest(setOf(PendingMigration("keystore-format"))))
        }
    }

    @Test
    fun `should fail if registered migrations have duplicate ids`() {
        // given
        val migrationRunner = MigrationRunner(
            setOf(
                testMigration(id = "duplicate", order = 1, calls = mutableListOf()),
                testMigration(id = "duplicate", order = 2, calls = mutableListOf()),
            ),
        )

        // when / then
        assertThrows<IllegalStateException> {
            migrationRunner.run(MigrationRequest.empty())
        }
    }

    private fun testMigration(id: String, order: Int, calls: MutableList<String>) = object : Migration {
        override val id = id
        override val order = order

        override fun run() {
            calls.add(id)
        }
    }
}
