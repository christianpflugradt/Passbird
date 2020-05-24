package de.pflugradts.pwman3;

import com.google.common.eventbus.Subscribe;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import de.pflugradts.pwman3.domain.model.ddd.AggregateRoot;
import de.pflugradts.pwman3.domain.model.ddd.DomainEntity;
import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.model.ddd.Repository;
import de.pflugradts.pwman3.domain.model.ddd.ValueObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

class PwMan3TestArch {
    
    private static final String ROOT = "de.pflugradts.pwman3";
    private static final String ADAPTER_ROOT = ROOT + ".adapter";
    private static final String APPLICATION_ROOT = ROOT + ".application";
    private static final String DOMAIN_ROOT = ROOT + ".domain";
    private static final String DOMAIN_MODELS = DOMAIN_ROOT + ".model";
    private static final String DOMAIN_SERVICES = DOMAIN_ROOT + ".service";

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
                .adapter(path(ADAPTER_ROOT, "clipboard"))
                .adapter(path(ADAPTER_ROOT, "exchange"))
                .adapter(path(ADAPTER_ROOT, "keystore"))
                .adapter(path(ADAPTER_ROOT, "passwordstore"))
                .adapter(path(ADAPTER_ROOT, "userinterface"));
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
    class UtilsTest {

        @Test
        void utilityMethodsShouldBeStatic() {
            methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Utils")
                    .should().beStatic()
                    .check(classes);
        }

        @Test
        void utilityConstantsShouldBeStaticAndFinal() {
            fields().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Utils")
                    .should().beStatic().andShould().beFinal()
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

    @Nested
    class BestPracticesTest {

        @Test
        void noMethodsShouldBeFinal() {
            noMethods().should().beFinal()
                    .check(classes);
        }

        @Test
        void noClassesShouldImplementCloneable() {
            noClasses().should().implement(Cloneable.class)
                    .check(classes);
        }

        @Test
        void instanceFieldsShouldBePrivate() {
            fields().that().areNotStatic().should().bePrivate()
                    .check(classes);
        }

    }

}
