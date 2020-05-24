package de.pflugradts.pwman3.adapter.clipboard;

import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
@Disabled // clipboard not accessible in headless environment
class ClipboardServiceTestIT {

    @Mock
    private Configuration configuration;
    @InjectMocks
    private ClipboardService clipboardService;

    @Test
    void shouldCopyMessageToClipboard() {
        // given
        final var message = "write this to clipboard";
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withClipboardResetDisabled().fake();

        // when
        clipboardService.post(Output.of(Bytes.of(message)));

        // then
        final var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(clipboard.getData(DataFlavor.stringFlavor))
                        .isNotNull().isEqualTo(message)
        );
    }

    @Test
    void shouldClearClipboard() {
        // given
        final var message = "write this to clipboard";
        final var delaySeconds = 1;
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withClipboardResetEnabled()
                .withClipboardResetDelaySeconds(delaySeconds).fake();

        // when
        clipboardService.post(Output.of(Bytes.of(message)));

        // then
        final var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(clipboard.getData(DataFlavor.stringFlavor))
                        .isNotNull().isEqualTo(message)
        );
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(clipboard.getData(DataFlavor.stringFlavor))
                        .isNotNull().isEqualTo("")
        );
    }

    @Test
    void shouldResetClearTimer() throws InterruptedException {
        // given
        final var message = "write this to clipboard";
        final var anotherMessage = "write this next";
        final var delaySeconds = 1;
        final var almostASecond = 800;
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withClipboardResetEnabled()
                .withClipboardResetDelaySeconds(delaySeconds).fake();

        // when
        clipboardService.post(Output.of(Bytes.of(message)));
        Thread.sleep(almostASecond);
        clipboardService.post(Output.of(Bytes.of(anotherMessage)));
        Thread.sleep(almostASecond);

        // then
        final var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(clipboard.getData(DataFlavor.stringFlavor))
                        .isNotNull().isEqualTo(anotherMessage)
        );
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(clipboard.getData(DataFlavor.stringFlavor))
                        .isNotNull().isEqualTo("")
        );
    }

}
