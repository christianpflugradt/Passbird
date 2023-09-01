package de.pflugradts.passbird.application.configuration;

import de.pflugradts.passbird.domain.model.password.PasswordRequirements;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MockitoConfigurationFaker {

    private boolean isTemplate = false;
    private String passwordStoreLocation;
    private String keyStoreLocation;
    private boolean isVerifyChecksum;
    private boolean isVerifySignature;
    private boolean isClipboardResetEnabled = false;
    private int clipboardResetDelaySeconds = 10;
    private boolean isSecureInput = false;
    private int passwordLength = 20;
    private boolean hasSpecialCharacters = false;
    private boolean isPromptOnRemoval = false;
    private Configuration configuration;

    public static MockitoConfigurationFaker faker() {
        return new MockitoConfigurationFaker();
    }

    public MockitoConfigurationFaker forInstance(final Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public MockitoConfigurationFaker withConfigurationTemplate() {
        this.isTemplate = true;
        return this;
    }

    public MockitoConfigurationFaker withPasswordStoreLocation(final String passwordStoreLocation) {
        this.passwordStoreLocation = passwordStoreLocation;
        return this;
    }

    public MockitoConfigurationFaker withKeyStoreLocation(final String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
        return this;
    }

    public MockitoConfigurationFaker withClipboardResetEnabled() {
        this.isClipboardResetEnabled = true;
        return this;
    }

    public MockitoConfigurationFaker withClipboardResetDisabled() {
        this.isClipboardResetEnabled = false;
        return this;
    }

    public MockitoConfigurationFaker withClipboardResetDelaySeconds(int seconds) {
        this.clipboardResetDelaySeconds = seconds;
        return this;
    }

    public MockitoConfigurationFaker withSecureInputEnabled() {
        this.isSecureInput = true;
        return this;
    }

    public MockitoConfigurationFaker withSecureInputDisabled() {
        this.isSecureInput = false;
        return this;
    }

    public MockitoConfigurationFaker withPasswordLength(final int passwordLength) {
        this.passwordLength = passwordLength;
        return this;
    }

    public MockitoConfigurationFaker withSpecialCharactersEnabled() {
        this.hasSpecialCharacters = true;
        return this;
    }

    public MockitoConfigurationFaker withSpecialCharactersDisabled() {
        this.hasSpecialCharacters = false;
        return this;
    }

    public MockitoConfigurationFaker withPromptOnRemovalEnabled() {
        this.isPromptOnRemoval = true;
        return this;
    }

    public MockitoConfigurationFaker withVerifyChecksumEnabled() {
        this.isVerifyChecksum = true;
        return this;
    }

    public MockitoConfigurationFaker withVerifySignatureEnabled() {
        this.isVerifySignature = true;
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
        lenient().when(configuration.isTemplate()).thenReturn(isTemplate);
        lenient().when(configuration.parsePasswordRequirements())
                .thenReturn(PasswordRequirements.of(hasSpecialCharacters, passwordLength));
        return configuration;
    }

    private Configuration.Clipboard givenClipboardAdapter() {
        final var clipboardReset = mock(Configuration.ClipboardReset.class);
        final var clipboard = mock(Configuration.Clipboard.class);
        lenient().when(clipboardReset.isEnabled()).thenReturn(isClipboardResetEnabled);
        if (isClipboardResetEnabled) {
            lenient().when(clipboardReset.getDelaySeconds()).thenReturn(clipboardResetDelaySeconds);
        }
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
        lenient().when(passwordProvider.getLength()).thenReturn(passwordLength);
        lenient().when(passwordProvider.isSpecialCharacters()).thenReturn(hasSpecialCharacters);
        lenient().when(passwordProvider.isPromptOnRemoval()).thenReturn(isPromptOnRemoval);
        return passwordProvider;
    }

    private Configuration.PasswordStore givenPasswordStoreAdapter() {
        final var passwordStore = mock(Configuration.PasswordStore.class);
        lenient().when(passwordStore.getLocation()).thenReturn(passwordStoreLocation);
        lenient().when(passwordStore.isVerifyChecksum()).thenReturn(isVerifyChecksum);
        lenient().when(passwordStore.isVerifySignature()).thenReturn(isVerifySignature);
        return passwordStore;
    }

    private Configuration.UserInterface givenUserInterfaceAdapter() {
        final var userInterface = mock(Configuration.UserInterface.class);
        lenient().when(userInterface.isSecureInput()).thenReturn(isSecureInput);
        return userInterface;
    }


}
