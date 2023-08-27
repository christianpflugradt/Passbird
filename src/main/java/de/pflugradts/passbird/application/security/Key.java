package de.pflugradts.passbird.application.security;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class Key {
    Bytes secret;
    Bytes iv;
}
