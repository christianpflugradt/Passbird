package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;

public interface PasswordProvider {
    Bytes createNewPassword();
}
