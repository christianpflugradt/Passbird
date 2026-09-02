package de.pflugradts.passbird.adapter.keystore
import java.security.KeyStore
private const val PKCS12_KEYSTORE = "PKCS12"
class KeyStoreFactory constructor() {
    val pkcs12Instance: KeyStore get() = KeyStore.getInstance(PKCS12_KEYSTORE)
}
