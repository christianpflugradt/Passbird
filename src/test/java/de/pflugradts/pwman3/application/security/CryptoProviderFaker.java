package de.pflugradts.pwman3.application.security;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.control.Try;
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

    public CryptoProvider fake() {
        lenient().when(cryptoProvider.encrypt(any(Bytes.class))).thenAnswer(
                invocation -> Try.of(() -> invocation.getArgument(0)));
        lenient().when(cryptoProvider.decrypt(any(Bytes.class))).thenAnswer(
                invocation -> Try.of(() -> invocation.getArgument(0)));
        return cryptoProvider;
    }

}
