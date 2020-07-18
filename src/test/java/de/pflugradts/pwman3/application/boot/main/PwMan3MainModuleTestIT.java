package de.pflugradts.pwman3.application.boot.main;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import de.pflugradts.pwman3.application.commandhandling.handler.CustomSetCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.DiscardCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ExportCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.GetCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.HelpCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ImportCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ListCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.QuitCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.SetCommandHandler;
import de.pflugradts.pwman3.application.commandhandling.handler.ViewCommandHandler;
import de.pflugradts.pwman3.application.eventhandling.ApplicationEventHandler;
import de.pflugradts.pwman3.domain.service.eventhandling.DomainEventHandler;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PwMan3MainModuleTestIT {

    @Test
    void shouldResolveAllDependencies() {
        // given / when
        final var actual = Guice.createInjector(Modules.override(new ApplicationModule()).with(new PwMan3TestModule()))
                .getInstance(PwMan3TestMain.class);

        // then
        assertThat(actual).isNotNull();
        assertAllPropertiesAreBound(actual);
        assertThatAllCommandHandlersAreBound(actual);
        assertThatAllEventHandlersAreBound(actual);
    }

    private static void assertAllPropertiesAreBound(PwMan3TestMain pwMan3TestMain) {
        assertThat(pwMan3TestMain.getBootable()).isNotNull().isInstanceOf(PwMan3Application.class);
        assertThat(pwMan3TestMain.getClipboardAdapterPort()).isNotNull();
        assertThat(pwMan3TestMain.getConfiguration()).isNotNull();
        assertThat(pwMan3TestMain.getEventRegistry()).isNotNull();
        assertThat(pwMan3TestMain.getImportExportService()).isNotNull();
        assertThat(pwMan3TestMain.getKeyStoreAdapterPort()).isNotNull();
        assertThat(pwMan3TestMain.getPasswordProvider()).isNotNull();
        assertThat(pwMan3TestMain.getPasswordService()).isNotNull();
        assertThat(pwMan3TestMain.getPasswordStoreAdapterPort()).isNotNull();
        assertThat(pwMan3TestMain.getUserInterfaceAdapterPort()).isNotNull();
    }

    private static void assertThatAllCommandHandlersAreBound(PwMan3TestMain pwMan3TestMain) {
        assertThat(pwMan3TestMain.getCommandHandlers()).isNotNull()
                .extracting("class")
                .containsExactlyInAnyOrder(
                        CustomSetCommandHandler.class,
                        DiscardCommandHandler.class,
                        ExportCommandHandler.class,
                        GetCommandHandler.class,
                        HelpCommandHandler.class,
                        ImportCommandHandler.class,
                        ListCommandHandler.class,
                        QuitCommandHandler.class,
                        SetCommandHandler.class,
                        ViewCommandHandler.class);
    }

    private static void assertThatAllEventHandlersAreBound(PwMan3TestMain pwMan3TestMain) {
        assertThat(pwMan3TestMain.getEventHandlers()).isNotNull()
                .extracting("class")
                .containsExactlyInAnyOrder(
                        ApplicationEventHandler.class,
                        DomainEventHandler.class);
    }

    static class PwMan3TestModule extends AbstractModule {
        @Override
        public void configure() {
            bind(CryptoProvider.class).toInstance(mock(CryptoProvider.class));
        }
    }

}
