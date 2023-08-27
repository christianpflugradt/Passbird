package de.pflugradts.passbird.application.failurehandling.failure;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PasswordEntryFailure implements Failure {
    Bytes bytes;
    Throwable throwable;
}
