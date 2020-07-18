package de.pflugradts.pwman3.application.security;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class Key {
    Bytes secret;
    Bytes iv;
}
