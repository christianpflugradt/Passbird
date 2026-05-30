package de.pflugradts.passbird.adapter.keystore

import jakarta.inject.Inject
import java.security.KeyStore

private const val JCEKS_KEYSTORE = "JCEKS"
private const val PKCS12_KEYSTORE = "PKCS12"

class KeyStoreFactory @Inject constructor() {
    val jceksInstance: KeyStore get() = KeyStore.getInstance(JCEKS_KEYSTORE)
    val pkcs12Instance: KeyStore get() = KeyStore.getInstance(PKCS12_KEYSTORE)
}
