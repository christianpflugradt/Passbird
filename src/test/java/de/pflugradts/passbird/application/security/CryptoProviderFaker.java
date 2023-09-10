package de.pflugradts.passbird.application.security;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CryptoProviderFaker {

    private CryptoProvider cryptoProvider;

    public static CryptoProviderFaker faker() {
        return new CryptoProviderFaker();
    }

    public CryptoProviderFaker forInstance(final CryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
        return this;
    }

    public CryptoProviderFaker withMockedEncryption() {
        lenient().when(cryptoProvider.encrypt(any(Bytes.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        lenient().when(cryptoProvider.decrypt(any(Bytes.class))).thenAnswer(
                invocation -> invocation.getArgument(0));
        return this;
    }

    public CryptoProvider fake() {
        return cryptoProvider;
    }

}
