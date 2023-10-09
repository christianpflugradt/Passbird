package de.pflugradts.passbird.adapter.passwordstore

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.failurehandling.FailureCollector
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.fakePasswordEntry
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._1
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._2
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._3
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._4
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._5
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._6
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._7
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._8
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot._9
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.service.NamespaceServiceFake
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import strikt.java.exists
import java.io.File
import java.util.UUID

internal class PasswordStoreFacadeIT {

    private val configuration = mockk<Configuration>()
    private val cryptoProvider = mockk<CryptoProvider>()
    private val failureCollector = mockk<FailureCollector>()
    private val namespaceServiceFake = NamespaceServiceFake()
    private val systemOperation = SystemOperation()
    private var passwordStoreFacade: PasswordStoreFacade = PasswordStoreFacade(
        passwordStoreReader = PasswordStoreReader(
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            failureCollector = failureCollector,
            namespaceService = namespaceServiceFake,
            systemOperation = systemOperation,
        ),
        passwordStoreWriter = PasswordStoreWriter(
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            namespaceService = namespaceServiceFake,
            systemOperation = systemOperation,
        ),
    )

    private var tempPasswordStoreDirectory = UUID.randomUUID().toString()
    private var databaseFilename = tempPasswordStoreDirectory + File.separator + ReadableConfiguration.DATABASE_FILENAME

    @BeforeEach
    fun setup() {
        expectThat(File(tempPasswordStoreDirectory).mkdir()).isTrue()
        fakeConfiguration(instance = configuration, withPasswordStoreLocation = tempPasswordStoreDirectory)
        every { cryptoProvider.encrypt(any(Bytes::class)) } answers { firstArg() }
        every { cryptoProvider.decrypt(any(Bytes::class)) } answers { firstArg() }
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(databaseFilename).delete()).isTrue()
        expectThat(File(tempPasswordStoreDirectory).delete()).isTrue()
    }

    @Test
    fun `should write to and them read from database`() {
        // given
        val passwordEntry1 = fakePasswordEntry(withKeyBytes = bytesOf("key1"), withPasswordBytes = bytesOf("password1"))
        val passwordEntry2 = fakePasswordEntry(withKeyBytes = bytesOf("key2"), withPasswordBytes = bytesOf("password2"))
        val passwordEntry3 = fakePasswordEntry(withKeyBytes = bytesOf("key3"), withPasswordBytes = bytesOf("password3"))
        val passwordEntries = listOf(passwordEntry1, passwordEntry2, passwordEntry3)

        // when
        passwordStoreFacade.sync { passwordEntries.stream() }
        expectThat(File(databaseFilename)).exists()
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly passwordEntries
    }

    @Test
    fun `should write to and them read from database using namespaces`() {
        // given
        val namespace1 = bytesOf("namespace1")
        val namespace3 = bytesOf("Namespace3")
        val namespace9 = bytesOf("+nameSpace*9")
        namespaceServiceFake.deploy(namespace1, _1)
        namespaceServiceFake.deploy(namespace3, _3)
        namespaceServiceFake.deploy(namespace9, _9)
        val passwordEntry1 = fakePasswordEntry(
            withKeyBytes = bytesOf("key1"),
            withPasswordBytes = bytesOf("password1"),
            withNamespace = DEFAULT,
        )
        val passwordEntry2 = fakePasswordEntry(
            withKeyBytes = bytesOf("key2"),
            withPasswordBytes = bytesOf("password2"),
            withNamespace = _1,
        )
        val passwordEntry3 = fakePasswordEntry(
            withKeyBytes = bytesOf("key3"),
            withPasswordBytes = bytesOf("password3"),
            withNamespace = _3,
        )
        val passwordEntry3b = fakePasswordEntry(
            withKeyBytes = bytesOf("key3"),
            withPasswordBytes = bytesOf("password3b"),
            withNamespace = _9,
        )
        val passwordEntries = listOf(passwordEntry1, passwordEntry2, passwordEntry3, passwordEntry3b)

        // when
        passwordStoreFacade.sync { passwordEntries.stream() }
        namespaceServiceFake.reset()
        expectThat(File(databaseFilename)).exists()
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().toList()) containsExactly passwordEntries
        listOf(_2, _4, _5, _6, _7, _8).forEach { expectThat(namespaceServiceFake.atSlot(it).isPresent).isFalse() }
        mapOf(
            _1 to namespace1,
            _3 to namespace3,
            _9 to namespace9,
        ).forEach { (k, v) ->
            expectThat(namespaceServiceFake.atSlot(k).isPresent)
            expectThat(namespaceServiceFake.atSlot(k).get().bytes) isEqualTo v
        }
    }

    @Test
    fun `should write to and them read from empty database`() {
        // given
        expectThat(File(databaseFilename).createNewFile()).isTrue()

        // when
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }

    @Test
    fun `should create empty password database if file not exists`() {
        // given
        expectThat(File(databaseFilename).createNewFile()).isTrue()

        // when
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }

    @Test
    fun `should create empty password database if file is empty`() {
        // given
        expectThat(File(databaseFilename).createNewFile()).isTrue()

        // when
        val actual = passwordStoreFacade.restore()

        // then
        expectThat(actual.get().count()) isEqualTo 0
    }
}
