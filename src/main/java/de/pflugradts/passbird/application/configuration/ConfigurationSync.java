package de.pflugradts.passbird.application.configuration;

import io.vavr.control.Try;

public interface ConfigurationSync {
    Try<Void> sync(String directory);
}
