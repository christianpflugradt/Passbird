package de.pflugradts.pwman3.adapter.clipboard;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.application.ClipboardAdapterPort;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import io.vavr.control.Try;
import java.util.Objects;

@Singleton
public class ClipboardService implements ClipboardAdapterPort {

    private static final long MILLI_SECONDS = 1000L;

    @Inject
    private SystemOperation systemOperation;
    @Inject
    private ReadableConfiguration configuration;

    private Thread cleanerThread;

    @Override
    public void post(final Output output) {
        this.terminateCleaner();
        systemOperation.copyToClipboard(output.getBytes().asString());
        this.scheduleCleaner();
    }

    private void terminateCleaner() {
        if (Objects.nonNull(cleanerThread)) {
            cleanerThread.interrupt();
        }
    }

    private void scheduleCleaner() {
        if (isResetEnabledInConfiguration()) {
            cleanerThread = new Thread(() -> sleep().onSuccess(x -> systemOperation.copyToClipboard("")));
            cleanerThread.start();
        }
    }

    private Try<Void> sleep() {
        return Try.run(() -> {
            try {
                Thread.sleep(getDelaySecondsFromConfiguration() * MILLI_SECONDS);
            } catch (InterruptedException ex) {
                // rethrow as unchecked exception since Vavr currently prints checked exceptions to std err
                throw new UncheckedExecutionException(ex);
            }
        });
    }

    private boolean isResetEnabledInConfiguration() {
        return configuration.getAdapter().getClipboard().getReset().isEnabled();
    }

    private int getDelaySecondsFromConfiguration() {
        return configuration.getAdapter().getClipboard().getReset().getDelaySeconds();
    }

}
