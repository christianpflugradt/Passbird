package de.pflugradts.pwman3.domain.model.event;

import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PasswordEntryNotFound implements DomainEvent {
    Bytes keyBytes;
}
