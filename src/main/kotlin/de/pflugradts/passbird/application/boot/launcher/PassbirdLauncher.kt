package de.pflugradts.passbird.application.boot.launcher

import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.boot.bootModule
import de.pflugradts.passbird.application.boot.main.ApplicationModule
import de.pflugradts.passbird.application.boot.setup.SetupModule
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

private const val COPYRIGHT = "\tCopyright 2020 - 2023 Christian Pflugradt"
private const val LICENSE = """\tThis software is licensed under the Apache License, Version 2.0 (APLv2)
\tYou may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
"""

class PassbirdLauncher @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject private val systemOperation: SystemOperation,
) : Bootable {

    private val keyStoreLocation get() = configuration.getAdapter().getKeyStore().getLocation()

    override fun boot() {
        sendLicenseNotice()
        sendBanner()
        bootModule(if (keystoreExists()) ApplicationModule() else SetupModule())
    }

    private fun keystoreExists() = keyStoreLocation.isNotEmpty() &&
        systemOperation.getPath(keyStoreLocation).resolve(ReadableConfiguration.KEYSTORE_FILENAME).toFile().exists()

    private fun sendBanner() {
        userInterfaceAdapterPort.sendLineBreak()
        userInterfaceAdapterPort.send(outputOf(bytesOf(banner())))
        userInterfaceAdapterPort.send(outputOf(bytesOf("\t${javaClass.getPackage().implementationVersion}")))
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun sendLicenseNotice() {
        userInterfaceAdapterPort.sendLineBreak()
        userInterfaceAdapterPort.send(outputOf(bytesOf(COPYRIGHT)))
        userInterfaceAdapterPort.send(outputOf(bytesOf(LICENSE)))
    }

    private fun banner() = byteArrayOf(
        0x9, 0x20, 0x5f, 0x5f, 0x5f, 0x5f, 0x5f, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
        0x20, 0x20, 0x5f, 0x5f, 0x20, 0x20, 0x5f, 0x5f, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
        0x20, 0x20, 0x20, 0x20, 0x5f, 0x5f, 0x5f, 0x5f, 0xa, 0x9, 0x7c, 0x20, 0x20, 0x5f, 0x5f, 0x20, 0x5c,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x7c, 0x20, 0x20, 0x5c, 0x2f, 0x20, 0x20, 0x7c,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x7c, 0x5f, 0x5f, 0x5f, 0x20, 0x5c,
        0xa, 0x9, 0x7c, 0x20, 0x7c, 0x5f, 0x5f, 0x29, 0x20, 0x7c, 0x5f, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
        0x5f, 0x7c, 0x20, 0x5c, 0x20, 0x20, 0x2f, 0x20, 0x7c, 0x20, 0x5f, 0x5f, 0x20, 0x5f, 0x20, 0x5f, 0x20,
        0x5f, 0x5f, 0x20, 0x20, 0x20, 0x5f, 0x5f, 0x29, 0x20, 0x7c, 0xa, 0x9, 0x7c, 0x20, 0x20, 0x5f, 0x5f,
        0x5f, 0x2f, 0x5c, 0x20, 0x5c, 0x20, 0x2f, 0x5c, 0x20, 0x2f, 0x20, 0x2f, 0x20, 0x7c, 0x5c, 0x2f, 0x7c,
        0x20, 0x7c, 0x2f, 0x20, 0x5f, 0x60, 0x20, 0x7c, 0x20, 0x27, 0x5f, 0x20, 0x5c, 0x20, 0x7c, 0x5f, 0x5f,
        0x20, 0x3c, 0xa, 0x9, 0x7c, 0x20, 0x7c, 0x20, 0x20, 0x20, 0x20, 0x20, 0x5c, 0x20, 0x56, 0x20, 0x20,
        0x56, 0x20, 0x2f, 0x7c, 0x20, 0x7c, 0x20, 0x20, 0x7c, 0x20, 0x7c, 0x20, 0x28, 0x5f, 0x7c, 0x20, 0x7c,
        0x20, 0x7c, 0x20, 0x7c, 0x20, 0x7c, 0x5f, 0x5f, 0x5f, 0x29, 0x20, 0x7c, 0xa, 0x9, 0x7c, 0x5f, 0x7c,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x5c, 0x5f, 0x2f, 0x5c, 0x5f, 0x2f, 0x20, 0x7c, 0x5f, 0x7c, 0x20,
        0x20, 0x7c, 0x5f, 0x7c, 0x5c, 0x5f, 0x5f, 0x2c, 0x5f, 0x7c, 0x5f, 0x7c, 0x20, 0x7c, 0x5f, 0x7c, 0x5f,
        0x5f, 0x5f, 0x5f, 0x2f, 0xa,
    )

    override fun terminate(systemOperation: SystemOperation) { systemOperation.exit() }
}