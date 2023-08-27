package de.pflugradts.passbird.application.failurehandling.failure;

import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class InputFailure implements Failure {
    Throwable throwable;
}
