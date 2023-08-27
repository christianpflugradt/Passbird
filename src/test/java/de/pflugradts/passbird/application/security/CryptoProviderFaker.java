package de.pflugradts.passbird.application.security;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import io.vavr.control.Try;
import javax.crypto.BadPaddingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
                invocation -> Try.of(() -> invocation.getArgument(0)));
        lenient().when(cryptoProvider.decrypt(any(Bytes.class))).thenAnswer(
                invocation -> Try.of(() -> invocation.getArgument(0)));
        return this;
    }

    public CryptoProviderFaker withEncryptionFailure() {
        given(cryptoProvider.encrypt(any(Bytes.class))).willReturn(Try.failure(new BadPaddingException()));
        return this;
    }

    public CryptoProviderFaker withDecryptionFailure() {
        given(cryptoProvider.encrypt(any(Bytes.class)))
                .willAnswer(invocation -> Try.of(() -> invocation.getArgument(0)));
        given(cryptoProvider.decrypt(any(Bytes.class))).willReturn(Try.failure(new BadPaddingException()));
        return this;
    }

    public CryptoProvider fake() {
        return cryptoProvider;
    }

}
