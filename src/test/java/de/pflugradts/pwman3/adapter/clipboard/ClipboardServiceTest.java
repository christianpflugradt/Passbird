package de.pflugradts.pwman3.adapter.clipboard;

import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ClipboardServiceTest {

    @Mock
    private SystemOperation systemOperation;
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
        then(systemOperation).should().copyToClipboard(message);
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
        then(systemOperation).should().copyToClipboard(message);
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                then(systemOperation).should().copyToClipboard(message)
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
        then(systemOperation).should().copyToClipboard(message);
        then(systemOperation).should(never()).copyToClipboard(eq(""));
        then(systemOperation).should().copyToClipboard(anotherMessage);
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                then(systemOperation).should().copyToClipboard(eq(""))
        );
    }

}
