package de.pflugradts.pwman3.application.failurehandling.failure;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class DecryptionFailure implements Failure {
    Bytes bytes;
    Throwable throwable;
}
