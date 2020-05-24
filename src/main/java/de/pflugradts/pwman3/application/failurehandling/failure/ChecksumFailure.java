package de.pflugradts.pwman3.application.failurehandling.failure;

import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class ChecksumFailure implements Failure {
    Byte actualChecksum;
    Byte expectedChecksum;
}
