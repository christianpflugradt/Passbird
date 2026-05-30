package de.pflugradts.passbird

import com.google.common.eventbus.Subscribe
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.base.DescribedPredicate.alwaysTrue
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.library.Architectures.onionArchitecture
import de.pflugradts.kotlinextensions.UtilityArchitectureHelper
import de.pflugradts.kotlinextensions.UtilityOptionalFixture
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.ddd.ValueObject
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.lang.reflect.Modifier

private const val ROOT = "de.pflugradts"
private const val PASSBIRD_ROOT = "$ROOT.passbird"
private const val KOTLIN_EXTENSIONS_ROOT = "$ROOT.kotlinextensions"
private const val OPTIONAL_BRIDGE = "$KOTLIN_EXTENSIONS_ROOT.OptionKt"
private const val ADAPTER_ROOT = "$PASSBIRD_ROOT.adapter"
private const val APPLICATION_ROOT = "$PASSBIRD_ROOT.application"
private const val BOOT_ROOT = "$APPLICATION_ROOT.boot"
private const val DOMAIN_ROOT = "$PASSBIRD_ROOT.domain"
private const val DOMAIN_MODELS = "$DOMAIN_ROOT.model"
private const val DOMAIN_SERVICES = "$DOMAIN_ROOT.service"
private const val CLIPBOARD_ADAPTER = "clipboard"
private const val EXCHANGE_ADAPTER = "exchange"
private const val KEYSTORE_ADAPTER = "keystore"
private const val PASSWORDTREE_ADAPTER = "passwordtree"
private const val USERINTERFACE_ADAPTER = "userinterface"

@Tag(ARCHITECTURE)
class PassbirdTest {
    private var productionClasses = ClassFileImporter().withImportOption(DoNotIncludeTests()).importPackages(ROOT)
    private var passbirdClasses = ClassFileImporter().withImportOption(DoNotIncludeTests()).importPackages(PASSBIRD_ROOT)
    private var allClasses = ClassFileImporter().importPackages(ROOT)
    private fun path(vararg segments: String) = "${segments.joinToString(".")}.."

    @Test
    fun `should have onion architecture`() {
        onionArchitecture()
            .domainModels(path(DOMAIN_MODELS))
            .domainServices(path(DOMAIN_SERVICES))
            .applicationServices(path(APPLICATION_ROOT))
            .adapter(CLIPBOARD_ADAPTER, path(ADAPTER_ROOT, CLIPBOARD_ADAPTER))
            .adapter(EXCHANGE_ADAPTER, path(ADAPTER_ROOT, EXCHANGE_ADAPTER))
            .adapter(KEYSTORE_ADAPTER, path(ADAPTER_ROOT, KEYSTORE_ADAPTER))
            .adapter(PASSWORDTREE_ADAPTER, path(ADAPTER_ROOT, PASSWORDTREE_ADAPTER))
            .adapter(USERINTERFACE_ADAPTER, path(ADAPTER_ROOT, USERINTERFACE_ADAPTER))
            .ignoreDependency(areCompositionRoots(), alwaysTrue())
            .check(passbirdClasses)
    }

    @Nested
    inner class UndesiredClassesTest {

        @Test
        fun `production imports should include kotlin extensions package`() {
            expectThat(productionClasses.map { it.packageName }.toSet()).contains(KOTLIN_EXTENSIONS_ROOT)
        }

        @Test
        fun `no production classes except option bridge should depend on Optional`() {
            noProductionClassesExceptOptionBridgeShouldDependOnOptional().check(productionClasses)
        }

        @Test
        fun `optional dependency guardrail catches utility package additions`() {
            val utilityClasses = ClassFileImporter().importClasses(UtilityOptionalFixture::class.java)

            assertThrows<AssertionError> {
                noProductionClassesExceptOptionBridgeShouldDependOnOptional().check(utilityClasses)
            }
        }

        @Test
        fun `no classes should depend on guice`() {
            noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.google.inject..")
                .check(allClasses)
        }

        @Test
        fun `no classes should depend on jakarta inject`() {
            noClasses()
                .should().dependOnClassesThat().resideInAPackage("jakarta.inject..")
                .check(allClasses)
        }

        @Test
        fun `command handling should not depend on thread local state`() {
            noClasses().that().resideInAPackage(path(APPLICATION_ROOT, "commandhandling"))
                .should().dependOnClassesThat().haveFullyQualifiedName("java.lang.ThreadLocal")
                .check(passbirdClasses)
        }

        @Test
        fun `system operation should expose only allowlisted file and runtime effects`() {
            val publicMethodNames = SystemOperation::class.java.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }
                .map { it.name.substringBefore("-") }
                .toSet()

            expectThat(publicMethodNames) isEqualTo setOf(
                "copyTo",
                "createDirectory",
                "delete",
                "exists",
                "exit",
                "getClock",
                "getFileNames",
                "getPath",
                "isDirectory",
                "newInputStream",
                "newOutputStream",
                "readBytesFromFile",
                "resolvePath",
                "writeBytesToSensitiveFile",
                "writeStringToSensitiveFile",
                "writeToSensitiveFile",
            )
        }

        @Test
        fun `clipboard platform access should stay in clipboard adapter`() {
            noClasses().that().resideOutsideOfPackage(path(ADAPTER_ROOT, CLIPBOARD_ADAPTER))
                .should().dependOnClassesThat().resideInAPackage("java.awt..")
                .check(passbirdClasses)
        }

        @Test
        fun `terminal input access should stay in userinterface adapter`() {
            noClasses().that().resideOutsideOfPackage(path(ADAPTER_ROOT, USERINTERFACE_ADAPTER))
                .should().accessField(System::class.java, "in")
                .check(passbirdClasses)

            noClasses().that().resideOutsideOfPackage(path(ADAPTER_ROOT, USERINTERFACE_ADAPTER))
                .should().callMethod(System::class.java, "console")
                .check(passbirdClasses)
        }

        @Test
        fun `keystore construction should stay in keystore adapter`() {
            noClasses().that().resideOutsideOfPackage(path(ADAPTER_ROOT, KEYSTORE_ADAPTER))
                .should().dependOnClassesThat().haveFullyQualifiedName("java.security.KeyStore")
                .check(passbirdClasses)
        }

        @Test
        fun `process exit should stay in system operation`() {
            noClasses().that(areNotSystemOperation())
                .should().callMethod(System::class.java, "exit", Int::class.javaPrimitiveType)
                .check(passbirdClasses)
        }
    }

    @Nested
    inner class MainTest {
        @Test
        fun `no functions outside of main should use main functions`() {
            methods().that().haveNameMatching("^main.*")
                .should().onlyBeCalled().byClassesThat().haveNameMatching(".*MainKt$")
                .check(passbirdClasses)
        }
    }

    @Nested
    inner class AdapterTest {
        @Test
        fun `adapter port implementations should be in adapter packages`() {
            classes().that()
                .areAssignableTo(JavaClass.Predicates.INTERFACES.and(simpleNameEndingWith("AdapterPort")))
                .and().areNotInterfaces()
                .should().resideInAPackage(path(ADAPTER_ROOT))
                .check(passbirdClasses)
        }

        @Test
        fun `no classes should be in adapter package`() {
            noClasses().should().resideInAPackage(ADAPTER_ROOT).check(passbirdClasses)
        }
    }

    @Nested
    inner class PersistenceCycleTest {
        @Test
        fun `password tree persistence should not depend on nest service`() {
            noClasses().that().haveFullyQualifiedName("$DOMAIN_SERVICES.password.tree.NestingGround")
                .or().haveFullyQualifiedName("$ADAPTER_ROOT.passwordtree.PasswordTreeReader")
                .or().haveFullyQualifiedName("$ADAPTER_ROOT.passwordtree.PasswordTreeWriter")
                .should().dependOnClassesThat().haveFullyQualifiedName("$DOMAIN_SERVICES.nest.NestService")
                .check(passbirdClasses)
        }

        @Test
        fun `nest service should not depend on egg repository`() {
            noClasses().that().haveFullyQualifiedName("$DOMAIN_SERVICES.nest.NestingGroundService")
                .should().dependOnClassesThat().haveFullyQualifiedName("$DOMAIN_SERVICES.password.tree.EggRepository")
                .check(passbirdClasses)
        }
    }

    @Nested
    inner class RepositoryAccessTest {
        @Test
        fun `repositories should only be accessed from domain services`() {
            classes().that().areAssignableTo(Repository::class.java)
                .should().onlyBeAccessed().byClassesThat(areDomainServicesOrCompositionRoots())
                .check(passbirdClasses)
        }
    }

    @Nested
    inner class DomainModelTest {
        @Test
        fun `ddd package should only contain interfaces and abstract classes`() {
            classes().that().resideInAPackage(path(DOMAIN_MODELS, "ddd"))
                .should().beInterfaces().orShould().haveModifier(JavaModifier.ABSTRACT)
                .check(passbirdClasses)
        }

        @Test
        fun `aggregate roots should reside in domain model package`() {
            classes().that().areAssignableTo(AggregateRoot::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(passbirdClasses)
        }

        @Test
        fun `domain entities should reside in domain model package`() {
            classes().that().areAssignableTo(DomainEntity::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(passbirdClasses)
        }

        @Test
        fun `value objects should reside in domain model package`() {
            classes().that().areAssignableTo(ValueObject::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(passbirdClasses)
        }

        @Test
        fun `repositories should reside in domain model package`() {
            classes().that().areAssignableTo(Repository::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_SERVICES))
                .check(passbirdClasses)
        }

        @Test
        fun `domain events should reside in domain model event package`() {
            classes().that().areAssignableTo(DomainEvent::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS, "event"))
                .check(passbirdClasses)
        }

        @Test
        fun `no classes should be in domain package`() {
            noClasses().should().resideInAPackage(DOMAIN_ROOT)
                .check(passbirdClasses)
        }
    }

    @Nested
    inner class EventHandlerTest {
        @Test
        fun `event handlers should not have public methods`() {
            noMethods().that().areDeclaredInClassesThat().areAssignableTo(
                EventHandler::class.java,
            ).should().bePublic().check(passbirdClasses)
        }

        @Test
        fun `event handlers handle methods must be annotated with subscribe`() {
            methods().that().areDeclaredInClassesThat().areAssignableTo(EventHandler::class.java)
                .and().haveNameMatching("^handle.*")
                .and(areNotKotlinLambdas())
                .should().beAnnotatedWith(Subscribe::class.java)
                .check(passbirdClasses)
        }

        @Test
        fun `no methods that are not event handlers may be annotated with subscribe`() {
            noMethods().that().areDeclaredInClassesThat().areNotAssignableTo(EventHandler::class.java)
                .or().haveNameNotMatching("^handle.*")
                .should().beAnnotatedWith(Subscribe::class.java)
                .check(passbirdClasses)
        }
    }

    @Nested
    inner class NamingTest {
        @Test
        fun `no classes may have name ending with impl`() {
            noClassesMayHaveNameEndingWithImpl().check(productionClasses)
        }

        @Test
        fun `no classes may have name ending with helper`() {
            noClassesMayHaveNameEndingWithHelper().check(productionClasses)
        }

        @Test
        fun `naming guardrail catches utility package helper names`() {
            val utilityClasses = ClassFileImporter().importClasses(UtilityArchitectureHelper::class.java)

            assertThrows<AssertionError> { noClassesMayHaveNameEndingWithHelper().check(utilityClasses) }
        }
    }
}

private fun noProductionClassesExceptOptionBridgeShouldDependOnOptional(): ArchRule = noClasses().that(areNotTheOptionalBridge())
    .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Optional")

private fun noClassesMayHaveNameEndingWithImpl(): ArchRule = noClasses().should().haveSimpleNameEndingWith("Impl")

private fun noClassesMayHaveNameEndingWithHelper(): ArchRule = noClasses().should().haveSimpleNameEndingWith("Helper")

private fun areNotTheOptionalBridge() = object : DescribedPredicate<JavaClass>("are not the Optional to Option bridge") {
    override fun test(javaClass: JavaClass) = javaClass.name != OPTIONAL_BRIDGE
}

private fun areNotSystemOperation() = object : DescribedPredicate<JavaClass>("are not SystemOperation") {
    override fun test(javaClass: JavaClass) = javaClass.name != "$APPLICATION_ROOT.util.SystemOperation"
}

private fun areCompositionRoots() = object : DescribedPredicate<JavaClass>("are explicit composition roots") {
    override fun test(javaClass: JavaClass) = isCompositionRoot(javaClass)
}

private fun areDomainServicesOrCompositionRoots() =
    object : DescribedPredicate<JavaClass>("reside in domain services or are explicit composition roots") {
        override fun test(javaClass: JavaClass) = javaClass.packageName == DOMAIN_SERVICES ||
            javaClass.packageName.startsWith("$DOMAIN_SERVICES.") ||
            isCompositionRoot(javaClass)
    }

private fun isCompositionRoot(javaClass: JavaClass) =
    javaClass.packageName.startsWith("$BOOT_ROOT.") && javaClass.simpleName.endsWith("Graph")

private fun areNotKotlinLambdas() = object : DescribedPredicate<JavaMethod>("not a Kotlin lambda") {
    override fun test(method: JavaMethod) = !method.name.contains("$")
}
