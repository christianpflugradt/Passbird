package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.passbird.PROPERTY
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.property.PasswordTreeFixture
import de.pflugradts.passbird.property.normalizeEggs
import de.pflugradts.passbird.property.normalizeExplicitNests
import de.pflugradts.passbird.property.normalizeMemory
import de.pflugradts.passbird.property.normalizedEggs
import de.pflugradts.passbird.property.normalizedExplicitNests
import de.pflugradts.passbird.property.normalizedMemory
import de.pflugradts.passbird.property.orThrow
import de.pflugradts.passbird.property.passwordTreeFixtures
import de.pflugradts.passbird.property.populateNests
import de.pflugradts.passbird.property.toEggStreamSupplier
import io.mockk.mockk
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.Report
import net.jqwik.api.Reporting
import net.jqwik.api.Tag
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Files

@Tag(PROPERTY)
class PasswordTreeFacadePropertyTest {
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val passwordTreePayloadWriter = PasswordTreePayloadWriter()

    @Property(tries = 20)
    @Report(Reporting.FALSIFIED)
    fun preservesPasswordTreeStateAcrossSyncAndRestore(@ForAll("fixtures") fixture: PasswordTreeFixture) {
        val passwordTreeDirectory = Files.createTempDirectory("passbird-tree-property")

        try {
            val configuration = mockk<Configuration>()
            fakeConfiguration(
                instance = configuration,
                withPasswordTreeLocation = passwordTreeDirectory.toString(),
                withEggIdMemoryEnabled = true,
                withEggIdMemoryPersisted = true,
            )
            val cryptoProvider = createAesGcmCipherForTesting()
            val writerNestService = createNestServiceForTesting()
            val readerNestService = createNestServiceForTesting()
            val systemOperation = SystemOperation()
            val passwordTreeFacade = PasswordTreeFacade(
                passwordTreeReader = PasswordTreeReader(
                    systemOperation = systemOperation,
                    configuration = configuration,
                    nestService = readerNestService,
                    cryptoProvider = cryptoProvider,
                    passwordTreeEnvelope = passwordTreeEnvelope,
                    passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
                ),
                passwordTreeWriter = PasswordTreeWriter(
                    systemOperation = systemOperation,
                    configuration = configuration,
                    nestService = writerNestService,
                    cryptoProvider = cryptoProvider,
                    passwordTreeEnvelope = passwordTreeEnvelope,
                    passwordTreePayloadWriter = passwordTreePayloadWriter,
                ),
            )

            fixture.populateNests(writerNestService)
            passwordTreeFacade.sync(fixture.toEggStreamSupplier(cryptoProvider)).orThrow("password tree sync")
            val restoreResult = passwordTreeFacade.restore()

            expectThat(normalizeEggs(restoreResult.get().toList(), cryptoProvider)) isEqualTo fixture.normalizedEggs()
            expectThat(normalizeExplicitNests(readerNestService)) isEqualTo fixture.normalizedExplicitNests()
            expectThat(normalizeMemory(restoreResult.memory(), cryptoProvider)) isEqualTo fixture.normalizedMemory()
        } finally {
            passwordTreeDirectory.toFile().deleteRecursively()
        }
    }

    @Provide
    fun fixtures() = passwordTreeFixtures()
}
