package de.pflugradts.passbird

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.nio.file.Files
import java.nio.file.Path

class CommitMessageHookTest {

    private val projectDirectory = Path.of("").toAbsolutePath()
    private val hookPath = projectDirectory.resolve(".git/hooks/commit-msg")

    @Test
    fun `should accept valid scoped commit message`() {
        val result = runHook("fix(passwordtree): preserve checksum verification on restore")

        expectThat(result.exitCode) isEqualTo 0
    }

    @Test
    fun `should reject unsupported scope`() {
        val result = runHook("fix(module): fix bug #42")

        expectThat(result.exitCode) isEqualTo 1
        expectThat(result.output) contains "ERROR: Invalid commit scope"
    }

    @ParameterizedTest
    @ValueSource(strings = ["fix(my scope): slips through", "fix(path/sub): slips through"])
    fun `should reject malformed scope grammar`(message: String) {
        val result = runHook(message)

        expectThat(result.exitCode) isEqualTo 1
        expectThat(result.output) contains "ERROR: Invalid commit message"
    }

    private fun runHook(firstLine: String): HookExecutionResult {
        val messageFile = Files.createTempFile("commit-message", ".txt")
        return try {
            Files.writeString(messageFile, "$firstLine\n")
            val process = ProcessBuilder("bash", hookPath.toString(), messageFile.toString())
                .directory(projectDirectory.toFile())
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }

            HookExecutionResult(exitCode = process.waitFor(), output = output)
        } finally {
            Files.deleteIfExists(messageFile)
        }
    }

    private data class HookExecutionResult(
        val exitCode: Int,
        val output: String,
    )
}
