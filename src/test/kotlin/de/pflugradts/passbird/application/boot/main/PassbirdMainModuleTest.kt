package de.pflugradts.passbird.application.boot.main

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.util.Modules
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.lang.reflect.Modifier

@Tag(INTEGRATION)
class PassbirdMainModuleTest {
    @Test
    fun `should resolve all dependencies`() {
        // given / when
        val runContext = PassbirdRunContext("/tmp".toDirectory(), Slot.DEFAULT)
        val actual = Guice.createInjector(Modules.override(ApplicationModule(runContext)).with(PassbirdTestModule()))
            .getInstance(PassbirdTestMain::class.java)

        // then
        expectThat(actual.bootable).isA<PassbirdApplication>()
        expectThat(actual.runContext) isSameInstanceAs runContext
        expectThat(actual.commandHandlers.implementationClasses()) isEqualTo expectedMultibinderClasses(CommandHandler::class.java)
        expectThat(actual.eventHandlers.implementationClasses()) isEqualTo expectedMultibinderClasses(
            EventHandler::class.java,
            CommandHandler::class.java,
        )
        expectThat(actual.initializers.implementationClasses()) isEqualTo expectedMultibinderClasses(Initializer::class.java)
        expectThat(actual.finalizers.implementationClasses()) isEqualTo expectedMultibinderClasses(Finalizer::class.java)
    }

    class PassbirdTestModule : AbstractModule() {
        public override fun configure() {
            bind(CryptoProvider::class.java).toInstance(mockk<CryptoProvider>())
        }
    }

    companion object {
        private const val ROOT = "de.pflugradts.passbird"
        private val productionClasses = ClassFileImporter().withImportOption(DoNotIncludeTests()).importPackages(ROOT)

        private fun expectedMultibinderClasses(type: Class<*>, vararg excludedTypes: Class<*>) = productionClasses
            .map { Class.forName(it.name) }
            .filter { it.isConcreteImplementationOf(type) }
            .filterNot { candidate -> excludedTypes.any { it.isAssignableFrom(candidate) } }
            .toSet()

        private fun Class<*>.isConcreteImplementationOf(type: Class<*>) =
            type.isAssignableFrom(this) && !isInterface && !Modifier.isAbstract(modifiers)
    }
}

private fun <T : Any> Iterable<T>.implementationClasses(): Set<Class<*>> = map { it::class.java }.toSet()
