package de.pflugradts.passbird

import com.google.common.eventbus.Subscribe
import com.google.inject.AbstractModule
import com.tngtech.archunit.base.DescribedPredicate.alwaysTrue
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.library.Architectures
import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.ddd.DomainEntity
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.ddd.ValueObject
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val ROOT = "de.pflugradts.passbird"
private const val ADAPTER_ROOT = "$ROOT.adapter"
private const val APPLICATION_ROOT = "$ROOT.application"
private const val DOMAIN_ROOT = "$ROOT.domain"
private const val DOMAIN_MODELS = "$DOMAIN_ROOT.model"
private const val DOMAIN_SERVICES = "$DOMAIN_ROOT.service"
private const val CLIPBOARD_ADAPTER = "clipboard"
private const val EXCHANGE_ADAPTER = "exchange"
private const val KEYSTORE_ADAPTER = "keystore"
private const val PASSWORDSTORE_ADAPTER = "passwordstore"
private const val USERINTERFACE_ADAPTER = "userinterface"

class PassbirdAT {
    private var classes = ClassFileImporter().withImportOption(DoNotIncludeTests()).importPackages(ROOT)
    private fun path(vararg segments: String) = "${segments.joinToString(".")}.."

    @Test
    fun shouldHaveOnionArchitecture() {
        Architectures.onionArchitecture()
            .domainModels(path(DOMAIN_MODELS))
            .domainServices(path(DOMAIN_SERVICES))
            .applicationServices(path(APPLICATION_ROOT))
            .adapter(CLIPBOARD_ADAPTER, path(ADAPTER_ROOT, CLIPBOARD_ADAPTER))
            .adapter(EXCHANGE_ADAPTER, path(ADAPTER_ROOT, EXCHANGE_ADAPTER))
            .adapter(KEYSTORE_ADAPTER, path(ADAPTER_ROOT, KEYSTORE_ADAPTER))
            .adapter(PASSWORDSTORE_ADAPTER, path(ADAPTER_ROOT, PASSWORDSTORE_ADAPTER))
            .adapter(USERINTERFACE_ADAPTER, path(ADAPTER_ROOT, USERINTERFACE_ADAPTER))
            .ignoreDependency(assignableTo(AbstractModule::class.java), alwaysTrue()) // exclude guice modules
            .check(classes)
    }

    @Nested
    inner class AdapterTest {
        @Test
        fun adapterPortImplementationsShouldBeInAdapterPackages() {
            classes().that()
                .areAssignableTo(JavaClass.Predicates.INTERFACES.and(simpleNameEndingWith("AdapterPort")))
                .and().areNotInterfaces()
                .should().resideInAPackage(path(ADAPTER_ROOT))
                .check(classes)
        }

        @Test
        fun noClassesShouldBeInAdapterPackage() {
            noClasses().should().resideInAPackage(ADAPTER_ROOT).check(classes)
        }
    }

    @Nested
    inner class RepositoryAccessTest {
        @Test
        fun repositoriesShouldOnlyBeAccessedFromApplicationAndDomainLayer() {
            classes().that().areAssignableTo(Repository::class.java)
                .should().onlyBeAccessed().byClassesThat().resideInAPackage(path(APPLICATION_ROOT))
                .orShould().onlyBeAccessed().byClassesThat().resideInAPackage(path(DOMAIN_SERVICES))
                .check(classes)
        }

        @Test
        fun repositoriesShouldOnlyBeAccessedFromDomainServices() {
            classes().that().areAssignableTo(Repository::class.java)
                .should().onlyBeAccessed().byClassesThat().resideInAPackage(path(DOMAIN_SERVICES))
                .check(classes)
        }
    }

    @Nested
    inner class DomainModelTest {
        @Test
        fun dddPackageShouldOnlyContainInterfaces() {
            classes().that().resideInAPackage(path(DOMAIN_MODELS, "ddd"))
                .should().beInterfaces()
                .check(classes)
        }

        @Test
        fun aggregateRootsShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(AggregateRoot::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(classes)
        }

        @Test
        fun domainEntitiesShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(DomainEntity::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(classes)
        }

        @Test
        fun valueObjectsShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(ValueObject::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(classes)
        }

        @Test
        fun repositoriesShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(Repository::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_SERVICES))
                .check(classes)
        }

        @Test
        @Disabled
        fun domainEventsShouldResideInDomainModelEventPackage() {
            classes().that().areAssignableFrom(DomainEvent::class.java).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS, "event"))
                .check(classes)
        }

        @Test
        fun noClassesShouldBeInDomainPackage() {
            noClasses().should().resideInAPackage(DOMAIN_ROOT)
                .check(classes)
        }
    }

    @Nested
    inner class EventHandlerTest {
        @Test
        fun eventHandlersShouldNotHavePublicMethods() {
            noMethods().that().areDeclaredInClassesThat().areAssignableTo(EventHandler::class.java).should().bePublic().check(classes)
        }

        @Test
        fun eventHandlersHandleMethodsMustBeAnnotatedWithSubscribe() {
            methods().that().areDeclaredInClassesThat().areAssignableTo(EventHandler::class.java)
                .and().haveNameMatching("handle.*")
                .should().beAnnotatedWith(Subscribe::class.java)
                .check(classes)
        }

        @Test
        fun noMethodsThatAreNotEventHandlersMayBeAnnotatedWithSubscribe() {
            noMethods().that().areDeclaredInClassesThat().areNotAssignableTo(EventHandler::class.java)
                .or().haveNameNotMatching("handle.*")
                .should().beAnnotatedWith(Subscribe::class.java)
                .check(classes)
        }
    }

    @Nested
    inner class NamingTest {
        @Test
        fun noClassesMayHaveNameEndingWithImpl() {
            noClasses().should().haveSimpleNameEndingWith("Impl").check(classes)
        }

        @Test
        fun noClassesMayHaveNameEndingWithHelper() {
            noClasses().should().haveSimpleNameEndingWith("Helper").check(classes)
        }
    }
}
