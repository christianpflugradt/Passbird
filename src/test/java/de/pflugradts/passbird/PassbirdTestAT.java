package de.pflugradts.passbird;

import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import de.pflugradts.passbird.domain.model.ddd.AggregateRoot;
import de.pflugradts.passbird.domain.model.ddd.DomainEntity;
import de.pflugradts.passbird.domain.model.ddd.DomainEvent;
import de.pflugradts.passbird.domain.model.ddd.Repository;
import de.pflugradts.passbird.domain.model.ddd.ValueObject;
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

class PassbirdTestAT {

    private static final String ROOT = "de.pflugradts.passbird";
    private static final String ADAPTER_ROOT = ROOT + ".adapter";
    private static final String APPLICATION_ROOT = ROOT + ".application";
    private static final String DOMAIN_ROOT = ROOT + ".domain";
    private static final String DOMAIN_MODELS = DOMAIN_ROOT + ".model";
    private static final String DOMAIN_SERVICES = DOMAIN_ROOT + ".service";

    private static final String CLIPBOARD_ADAPTER = "clipboard";
    private static final String EXCHANGE_ADAPTER = "exchange";
    private static final String KEYSTORE_ADAPTER = "keystore";
    private static final String PASSWORDSTORE_ADAPTER = "passwordstore";
    private static final String USERINTERFACE_ADAPTER = "userinterface";


    private JavaClasses classes;

    private String path(String... segments) {
        return String.join(".", segments) + "..";
    }

    @BeforeEach
    void setup() {
        classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(ROOT);
    }

    @Test
    void shouldHaveOnionArchitecture() {
        onionArchitecture()
            .domainModels(path(DOMAIN_MODELS))
            .domainServices(path(DOMAIN_SERVICES))
            .applicationServices(path(APPLICATION_ROOT))
            .adapter(CLIPBOARD_ADAPTER, path(ADAPTER_ROOT, CLIPBOARD_ADAPTER))
            .adapter(EXCHANGE_ADAPTER, path(ADAPTER_ROOT, EXCHANGE_ADAPTER))
            .adapter(KEYSTORE_ADAPTER, path(ADAPTER_ROOT, KEYSTORE_ADAPTER))
            .adapter(PASSWORDSTORE_ADAPTER, path(ADAPTER_ROOT, PASSWORDSTORE_ADAPTER))
            .adapter(USERINTERFACE_ADAPTER, path(ADAPTER_ROOT, USERINTERFACE_ADAPTER))
            .ignoreDependency(assignableTo(AbstractModule.class), alwaysTrue()) // exclude guice modules
            .check(classes);
    }

    @Nested
    class AdapterTest {

        @Test
        void adapterPortImplementationsShouldBeInAdapterPackages() {
            classes().that()
                .areAssignableTo(JavaClass.Predicates.INTERFACES.and(simpleNameEndingWith("AdapterPort")))
                .and().areNotInterfaces()
                .should().resideInAPackage(path(ADAPTER_ROOT))
                .check(classes);
        }

        @Test
        void noClassesShouldBeInAdapterPackage() {
            noClasses().should().resideInAPackage(ADAPTER_ROOT)
                .check(classes);
        }

    }

    @Nested
    class RepositoryAccessTest {

        @Test
        void repositoriesShouldOnlyBeAccessedFromApplicationAndDomainLayer() {
            classes().that().areAssignableTo(Repository.class)
                .should().onlyBeAccessed().byClassesThat().resideInAPackage(path(APPLICATION_ROOT))
                .orShould().onlyBeAccessed().byClassesThat().resideInAPackage(path(DOMAIN_SERVICES))
                .check(classes);
        }

        @Test
        void repositoriesShouldOnlyBeAccessedFromDomainServices() {
            classes().that().areAssignableTo(Repository.class)
                .should().onlyBeAccessed().byClassesThat().resideInAPackage(path(DOMAIN_SERVICES))
                .check(classes);
        }

    }

    @Nested
    class DomainModelTest {

        @Test
        void dddPackageShouldOnlyContainInterfaces() {
            classes().that().resideInAPackage(path(DOMAIN_MODELS, "ddd"))
                .should().beInterfaces()
                .check(classes);
        }

        @Test
        void aggregateRootsShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(AggregateRoot.class).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(classes);
        }

        @Test
        void domainEntitiesShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(DomainEntity.class).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(classes);
        }

        @Test
        void valueObjectsShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(ValueObject.class).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS))
                .check(classes);
        }

        @Test
        void repositoriesShouldResideInDomainModelPackage() {
            classes().that().areAssignableTo(Repository.class).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_SERVICES))
                .check(classes);
        }

        @Test
        @Disabled
        void domainEventsShouldResideInDomainModelEventPackage() {
            classes().that().areAssignableFrom(DomainEvent.class).and().areNotInterfaces()
                .should().resideInAPackage(path(DOMAIN_MODELS, "event"))
                .check(classes);
        }

        @Test
        void noClassesShouldBeInDomainPackage() {
            noClasses().should().resideInAPackage(DOMAIN_ROOT)
                .check(classes);
        }

    }

    @Nested
    class EventHandlerTest {

        @Test
        void eventHandlersShouldNotHavePublicMethods() {
            noMethods().that().areDeclaredInClassesThat().areAssignableTo(EventHandler.class)
                .should().bePublic().check(classes);
        }

        @Test
        void eventHandlersHandleMethodsMustBeAnnotatedWithSubscribe() {
            methods().that().areDeclaredInClassesThat().areAssignableTo(EventHandler.class)
                .and().haveNameMatching("handle.*")
                .should().beAnnotatedWith(Subscribe.class)
                .check(classes);
        }

        @Test
        void noMethodsThatAreNotEventHandlersMayBeAnnotatedWithSubscribe() {
            noMethods().that().areDeclaredInClassesThat().areNotAssignableTo(EventHandler.class)
                .or().haveNameNotMatching("handle.*")
                .should().beAnnotatedWith(Subscribe.class)
                .check(classes);
        }

    }

    @Nested
    class NamingTest {

        @Test
        void noClassesMayHaveNameEndingWithImpl() {
            noClasses().should().haveSimpleNameEndingWith("Impl")
                .check(classes);
        }

        @Test
        void noClassesMayHaveNameEndingWithHelper() {
            noClasses().should().haveSimpleNameEndingWith("Helper")
                .check(classes);
        }

    }
}
