package de.pflugradts.passbird.application.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MockitoConfigurationFaker {

    private boolean isTemplate = false;
    private boolean isPromptOnRemoval = false;
    private Configuration configuration;

    public static MockitoConfigurationFaker faker() {
        return new MockitoConfigurationFaker();
    }

    public MockitoConfigurationFaker forInstance(final Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public MockitoConfigurationFaker withPromptOnRemovalEnabled() {
        this.isPromptOnRemoval = true;
        return this;
    }

    public ReadableConfiguration fake() {
        final var adapter = mock(Configuration.Adapter.class);
        final var application = mock(Configuration.Application.class);
        final var clipboard = givenClipboardAdapter();
        final var keyStore = givenKeyStoreAdapter();
        final var passwordProvider = givenPasswordProvider();
        final var passwordStore = givenPasswordStoreAdapter();
        final var userInterface = givenUserInterfaceAdapter();
        lenient().when(adapter.getClipboard()).thenReturn(clipboard);
        lenient().when(adapter.getKeyStore()).thenReturn(keyStore);
        lenient().when(adapter.getPasswordStore()).thenReturn(passwordStore);
        lenient().when(adapter.getUserInterface()).thenReturn(userInterface);
        lenient().when(application.getPassword()).thenReturn(passwordProvider);
        lenient().when(configuration.getAdapter()).thenReturn(adapter);
        lenient().when(configuration.getApplication()).thenReturn(application);
        lenient().when(configuration.getTemplate()).thenReturn(isTemplate);
        return configuration;
    }

    private Configuration.Clipboard givenClipboardAdapter() {
        final var clipboardReset = mock(Configuration.ClipboardReset.class);
        final var clipboard = mock(Configuration.Clipboard.class);
        lenient().when(clipboard.getReset()).thenReturn(clipboardReset);
        return clipboard;
    }

    private Configuration.KeyStore givenKeyStoreAdapter() {
        final var keyStore = mock(Configuration.KeyStore.class);
        lenient().when(keyStore.getLocation()).thenReturn(keyStoreLocation);
        return keyStore;
    }

    private Configuration.Password givenPasswordProvider() {
        final var passwordProvider = mock(Configuration.Password.class);
        lenient().when(passwordProvider.getPromptOnRemoval()).thenReturn(isPromptOnRemoval);
        return passwordProvider;
    }

    private Configuration.PasswordStore givenPasswordStoreAdapter() {
        final var passwordStore = mock(Configuration.PasswordStore.class);
        lenient().when(passwordStore.getLocation()).thenReturn(passwordStoreLocation);
        lenient().when(passwordStore.getVerifyChecksum()).thenReturn(isVerifyChecksum);
        lenient().when(passwordStore.getVerifySignature()).thenReturn(isVerifySignature);
        return passwordStore;
    }

    private Configuration.UserInterface givenUserInterfaceAdapter() {
        return mock(Configuration.UserInterface.class);
    }


}
