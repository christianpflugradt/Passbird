package de.pflugradts.passbird.domain.model.event;

import de.pflugradts.passbird.domain.model.ddd.DomainEvent;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.Value;

@Value
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PasswordEntryNotFound implements DomainEvent {
    Bytes keyBytes;
}
