package de.pflugradts.pwman3.domain.model.event;

import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PasswordEntryCreated implements DomainEvent {
    PasswordEntry passwordEntry;
}
