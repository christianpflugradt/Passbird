package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.boot.launcher.LauncherModule;
import de.pflugradts.pwman3.application.util.GuiceInjector;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.CONFIGURATION_SYSTEM_PROPERTY;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public final class Main {

    private static final int EXPECTED_NUMBER_OF_ARGUMENTS = 1;

    private final GuiceInjector guiceInjector = new GuiceInjector();

    public static void main(final String... args) {
        new Main().boot(args);
    }

    void boot(final String... args) {
        if (args.length == EXPECTED_NUMBER_OF_ARGUMENTS) {
            System.setProperty(CONFIGURATION_SYSTEM_PROPERTY, args[0]);
        }
        guiceInjector.create(new LauncherModule()).getInstance(Bootable.class).boot();
    }

}
