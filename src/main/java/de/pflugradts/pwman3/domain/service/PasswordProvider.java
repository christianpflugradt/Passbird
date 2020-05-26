package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;

/**
 * A PasswordProvider generates new Passwords.
 */
public interface PasswordProvider {
    Bytes createNewPassword();
}
