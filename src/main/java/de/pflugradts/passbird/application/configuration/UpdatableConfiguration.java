package de.pflugradts.passbird.application.configuration;

public interface UpdatableConfiguration extends ReadableConfiguration {
    void updateDirectory(String directory);
}
