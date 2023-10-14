package de.pflugradts.passbird.application.security

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakePath
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import java.nio.file.Path

class CryptoProviderFactoryTest {

    private val application = mockk<Bootable>()
    private val configuration = mockk<Configuration>()
    private val keyStoreAdapterPort = mockk<KeyStoreAdapterPort>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val systemOperation = mockk<SystemOperation>()
    private val cryptoProviderFactory = CryptoProviderFactory(
        application = application,
        configuration = configuration,
        keyStoreAdapterPort = keyStoreAdapterPort,
        userInterfaceAdapterPort = userInterfaceAdapterPort,
        systemOperation = systemOperation,
    )

    @Test
    fun `should create crypto provider`() {
        // given
        val correctPassword = inputOf(bytesOf("letmein"))
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = fakePath()
        val keyStoreDirPath = fakePath(resolvingTo = Pair(keyStoreFilePath, ReadableConfiguration.KEYSTORE_FILENAME))
        fakeSystemOperation(instance = systemOperation, withPaths = listOf(Pair(keyStoreDirectory, keyStoreDirPath)))
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseSecureInputs = listOf(correctPassword))
        givenLoginSucceeds(correctPassword, keyStoreFilePath)

        // when
        val actual = cryptoProviderFactory.createCryptoProvider()

        // then
        expectThat(actual).isA<Cipherizer>()
    }

    @Test
    fun `should create crypto provider on 3rd password input attempt`() {
        // given
        val incorrectPassword1 = inputOf(bytesOf("letmeout"))
        val incorrectPassword2 = inputOf(bytesOf("letmeout"))
        val correctPassword = inputOf(bytesOf("letmein"))
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = fakePath()
        val keyStoreDirPath = fakePath(resolvingTo = Pair(keyStoreFilePath, ReadableConfiguration.KEYSTORE_FILENAME))
        fakeSystemOperation(instance = systemOperation, withPaths = listOf(Pair(keyStoreDirectory, keyStoreDirPath)))
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(incorrectPassword1, incorrectPassword2, correctPassword),
        )
        givenLoginFails(incorrectPassword1, keyStoreFilePath)
        givenLoginFails(incorrectPassword2, keyStoreFilePath)
        givenLoginSucceeds(correctPassword, keyStoreFilePath)

        // when
        val actual = cryptoProviderFactory.createCryptoProvider()

        // then
        expectThat(actual).isA<Cipherizer>()
    }

    @Test
    fun `should create crypto provider and terminate application after 3 failed attempts`() {
        // given
        val incorrectPassword = inputOf(bytesOf("letmeout"))
        val keyStoreDirectory = "tmp"
        val keyStoreFilePath = fakePath()
        val keyStoreDirPath = fakePath(resolvingTo = Pair(keyStoreFilePath, ReadableConfiguration.KEYSTORE_FILENAME))
        fakeSystemOperation(instance = systemOperation, withPaths = listOf(Pair(keyStoreDirectory, keyStoreDirPath)))
        fakeConfiguration(instance = configuration, withKeyStoreLocation = keyStoreDirectory)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(incorrectPassword, incorrectPassword, incorrectPassword),
        )
        givenLoginFails(incorrectPassword, keyStoreFilePath)
        every { application.terminate(systemOperation) } returns Unit

        // when
        cryptoProviderFactory.createCryptoProvider()

        // then
        verify(exactly = 1) { application.terminate(systemOperation) }
    }

    private fun givenLoginSucceeds(password: Input, keyStoreFilePath: Path) = every {
        keyStoreAdapterPort.loadKey(eq(password.bytes.toChars()), eq(keyStoreFilePath))
    } returns success(value = Key(emptyBytes(), emptyBytes()))

    private fun givenLoginFails(password: Input, keyStoreFilePath: Path) = every {
        keyStoreAdapterPort.loadKey(eq(password.bytes.toChars()), eq(keyStoreFilePath))
    } returns failure(ex = RuntimeException())
}
