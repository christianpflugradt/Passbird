package de.pflugradts.passbird.application.configuration

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class ConfigurationFactoryTest {
    private val homeDirectory = "/tmp"
    private val systemOperation = mockk<SystemOperation>()
    private val configurationFactory = ConfigurationFactory(
        systemOperation,
        PassbirdRunContext(homeDirectory.toDirectory(), Slot.DEFAULT),
    )

    @Test
    fun `should create default configuration when configuration file does not exist`() {
        // given
        fakeSystemOperation(
            instance = systemOperation,
            withDirectoryResolvingToFileName = Triple(
                homeDirectory.toDirectory(),
                ReadableConfiguration.CONFIGURATION_FILENAME.toFileName(),
                fakePath(),
            ),
        )
        every { systemOperation.exists(any<java.nio.file.Path>()) } returns false

        // when
        val captureSystemErr = CapturedOutputPrintStream.captureSystemErr()
        val actual = captureSystemErr.during { configurationFactory.loadConfiguration() }

        // then
        expectThat(actual.template).isTrue()
        expectThat(actual.application.password.length) isEqualTo 20
        expectThat(actual.application.yolk.algorithm) isEqualTo "SHA1"
        expectThat(actual.application.yolk.copyToClipboard).isTrue()
        expectThat(actual.application.yolk.digits) isEqualTo 6
        expectThat(actual.application.yolk.periodSeconds) isEqualTo 30
        expectThat(captureSystemErr.capture).isEqualTo("")
    }
}
