package de.pflugradts.pwman3.application.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class CryptoUtils {
    public static final String AES_CBC_PKCS5PADDING_CIPHER = "AES/CBC/PKCS5Padding";
    public static final String JCEKS_KEYSTORE = "JCEKS";
    public static final String AES_ENCRYPTION = "AES";
    public static final String SHA512_HASH = "SHA-512";
    public static final int KEYSTORE_KEY_BITS = 128;
    public static final int BLOCK_SIZE = 16;
}
