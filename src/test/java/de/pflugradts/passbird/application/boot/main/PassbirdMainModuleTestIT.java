package de.pflugradts.passbird.application.boot.main;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import de.pflugradts.passbird.application.commandhandling.handler.CustomSetCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.DiscardCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ExportCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.GetCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.RenameCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.SetCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.ViewCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AddNamespaceCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AssignNamespaceCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.SwitchNamespaceCommandHandler;
import de.pflugradts.passbird.application.commandhandling.handler.namespace.ViewNamespaceCommandHandler;
import de.pflugradts.passbird.application.eventhandling.ApplicationEventHandler;
import de.pflugradts.passbird.domain.service.eventhandling.DomainEventHandler;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PassbirdMainModuleTestIT {

    @Test
    void shouldResolveAllDependencies() {
        // given / when
        final var actual = Guice.createInjector(Modules.override(new ApplicationModule()).with(new PassbirdTestModule()))
                .getInstance(PassbirdTestMain.class);

        // then
        assertThat(actual).isNotNull();
        assertAllPropertiesAreBound(actual);
        assertThatAllCommandHandlersAreBound(actual);
        assertThatAllEventHandlersAreBound(actual);
    }

    private static void assertAllPropertiesAreBound(PassbirdTestMain passbirdTestMain) {
        assertThat(passbirdTestMain.getBootable()).isNotNull().isInstanceOf(PassbirdApplication.class);
        assertThat(passbirdTestMain.getClipboardAdapterPort()).isNotNull();
        assertThat(passbirdTestMain.getConfiguration()).isNotNull();
        assertThat(passbirdTestMain.getEventRegistry()).isNotNull();
        assertThat(passbirdTestMain.getImportExportService()).isNotNull();
        assertThat(passbirdTestMain.getKeyStoreAdapterPort()).isNotNull();
        assertThat(passbirdTestMain.getPasswordProvider()).isNotNull();
        assertThat(passbirdTestMain.getPasswordService()).isNotNull();
        assertThat(passbirdTestMain.getPasswordStoreAdapterPort()).isNotNull();
        assertThat(passbirdTestMain.getUserInterfaceAdapterPort()).isNotNull();
    }

    private static void assertThatAllCommandHandlersAreBound(PassbirdTestMain passbirdTestMain) {
        assertThat(passbirdTestMain.getCommandHandlers()).isNotNull()
                .extracting("class")
                .containsExactlyInAnyOrder(
                    AddNamespaceCommandHandler.class,
                    AssignNamespaceCommandHandler.class,
                    CustomSetCommandHandler.class,
                    DiscardCommandHandler.class,
                    ExportCommandHandler.class,
                    GetCommandHandler.class,
                    HelpCommandHandler.class,
                    ImportCommandHandler.class,
                    ListCommandHandler.class,
                    QuitCommandHandler.class,
                    RenameCommandHandler.class,
                    SetCommandHandler.class,
                    SwitchNamespaceCommandHandler.class,
                    ViewCommandHandler.class,
                    ViewNamespaceCommandHandler.class);
    }

    private static void assertThatAllEventHandlersAreBound(PassbirdTestMain passbirdTestMain) {
        assertThat(passbirdTestMain.getEventHandlers()).isNotNull()
                .extracting("class")
                .containsExactlyInAnyOrder(
                        ApplicationEventHandler.class,
                        DomainEventHandler.class);
    }

    static class PassbirdTestModule extends AbstractModule {
        @Override
        public void configure() {
            bind(CryptoProvider.class).toInstance(mock(CryptoProvider.class));
        }
    }

}
