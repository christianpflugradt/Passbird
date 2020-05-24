package de.pflugradts.pwman3.application.configuration;

import io.vavr.control.Try;

public interface ConfigurationSync {
    Try<Void> sync(String directory);
}
