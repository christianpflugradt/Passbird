package de.pflugradts.pwman3.application.configuration;

public interface UpdatableConfiguration extends ReadableConfiguration {
    void updateDirectory(String directory);
}
