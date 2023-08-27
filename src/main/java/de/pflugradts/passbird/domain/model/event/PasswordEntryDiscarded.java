package de.pflugradts.passbird.domain.model.event;

import de.pflugradts.passbird.domain.model.ddd.DomainEvent;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PasswordEntryDiscarded implements DomainEvent {
    PasswordEntry passwordEntry;
}
