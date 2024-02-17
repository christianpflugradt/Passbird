package de.pflugradts.passbird.application

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.launcher.LauncherModule
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.slot.Slot.S6
import de.pflugradts.passbird.domain.model.slot.Slot.S7
import de.pflugradts.passbird.domain.model.slot.Slot.S8
import de.pflugradts.passbird.domain.model.slot.Slot.S9
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.stream.Stream

class MainTest {

    val systemOperation = mockk<SystemOperation>()

    @BeforeEach
    fun setup() {
        mockMain(systemOperationMock = systemOperation, withMockedFileCheck = false)
    }

    @AfterEach
    fun cleanup() {
        unmockMain(withMockedFileCheck = false)
    }

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

    @ParameterizedTest
    @MethodSource("expectedNestSlotMapping")
    fun `should persist initial nest`(givenParam: String, expectedInitialSlot: Slot) {
        // given
        val givenHome = "/foo"
        every { systemOperation.exists(givenHome.toDirectory()) } returns true
        every { systemOperation.isDirectory(givenHome.toDirectory()) } returns true

        // when
        main(arrayOf(givenHome, givenParam))

        // then
        verify(exactly = 1) { bootModule(any(LauncherModule::class)) }
        expectThat(Global.initialSlot) isEqualTo expectedInitialSlot
    }

    companion object {
        @JvmStatic
        private fun expectedNestSlotMapping() = Stream.of(
            Arguments.of("1", S1),
            Arguments.of("2", S2),
            Arguments.of("3", S3),
            Arguments.of("4", S4),
            Arguments.of("5", S5),
            Arguments.of("6", S6),
            Arguments.of("7", S7),
            Arguments.of("8", S8),
            Arguments.of("9", S9),
            Arguments.of("", DEFAULT),
            Arguments.of("0", DEFAULT),
            Arguments.of("-1", DEFAULT),
            Arguments.of("10", DEFAULT),
            Arguments.of("foo", DEFAULT),
            Arguments.of("n1", DEFAULT),
        )
    }
}
