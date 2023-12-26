package de.pflugradts.passbird.application

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.util.SystemOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MainTest {

    val systemOperation = mockk<SystemOperation>()

    @BeforeEach
    fun setup() { mockMain(systemOperationMock = systemOperation, withMockedFileCheck = false) }

    @AfterEach
    fun cleanup() { unmockMain(withMockedFileCheck = false) }

    @Test
    fun `should set home to args and boot launcher`() {
        // given
        val givenHome = "/foo"
        every { systemOperation.exists(givenHome.toDirectory()) } returns true
        every { systemOperation.isDirectory(givenHome.toDirectory()) } returns true

        // when
        main(arrayOf(givenHome))

        // then
        verify(exactly = 1) { bootModule(any(LauncherModule::class)) }
        expectThat(Global.homeDirectory) isEqualTo givenHome.toDirectory()
    }

    @Test
    fun `should exit if no home directory is passed`() {
        // given
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        captureSystemErr.during {
            main(emptyArray())
        }

        // then
        expectThat(captureSystemErr.capture) isEqualTo "Shutting down: No home directory was given upon starting Passbird.\n"
        verify(exactly = 1) { systemOperation.exit() }
    }

    @Test
    fun `should exit if specified home directory does not exist`() {
        // given
        val givenHome = "/foo"
        every { systemOperation.exists(givenHome.toDirectory()) } returns false
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        captureSystemErr.during {
            main(arrayOf(givenHome))
        }

        // then
        expectThat(captureSystemErr.capture) isEqualTo "Shutting down: Specified home directory does not exist: $givenHome\n"
        verify(exactly = 1) { systemOperation.exit() }
    }

    @Test
    fun `should exit if specified home directory is not a directory`() {
        // given
        val givenHome = "/foo"
        every { systemOperation.exists(givenHome.toDirectory()) } returns true
        every { systemOperation.isDirectory(givenHome.toDirectory()) } returns false
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()

        // when
        captureSystemErr.during {
            main(arrayOf(givenHome))
        }

        // then
        expectThat(captureSystemErr.capture) isEqualTo "Shutting down: Specified home directory is actually not a directory: $givenHome\n"
        verify(exactly = 1) { systemOperation.exit() }
    }
}
