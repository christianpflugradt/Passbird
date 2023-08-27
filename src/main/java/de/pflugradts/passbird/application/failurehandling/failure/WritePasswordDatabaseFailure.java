package de.pflugradts.passbird.application.failurehandling.failure;

import lombok.Value;

import java.nio.file.Path;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class WritePasswordDatabaseFailure implements Failure {
    Path path;
    Throwable throwable;
}
