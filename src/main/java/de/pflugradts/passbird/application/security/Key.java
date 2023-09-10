package de.pflugradts.passbird.application.security;

import de.pflugradts.passbird.domain.model.transfer.Bytes;

public class Key {
    public final Bytes secret;
    public final Bytes iv;

    public Key(Bytes secret, Bytes iv) {
        this.secret = secret;
        this.iv = iv;
    }

    public Bytes getSecret() {
        return secret;
    }

    public Bytes getIv() {
        return iv;
    }
}
