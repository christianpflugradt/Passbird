package de.pflugradts.passbird.application.boot.setup

import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

class SetupGuide @Inject constructor(
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) {
    fun sendWelcome() {
        send("Welcome to Passbird Setup!")
        userInterfaceAdapterPort.sendLineBreak()
    }

    fun sendConfigTemplateRouteInformation() {
        send("You have landed here because you did not provide a configuration.")
        send("If you have a configuration file, then please start Passbird as follows:")
        send("java -jar passbird.jar \"absolute-path-to-directory-with-configuration-file\"")
        userInterfaceAdapterPort.sendLineBreak()
        send("Example for a Linux path: /etc/passbird")
        send("Example for a Windows path: c:\\programs\\passbird")
        send("The configuration file must be in the specified directory and it must be named 'passbird.yml.'")
        userInterfaceAdapterPort.sendLineBreak()
        send("To (c)ontinue setup, press 'c'. To quit setup, press any other key")
    }

    fun sendConfigKeyStoreRouteInformation(location: String) {
        send("You have landed here because the keystore specified in your configuration does not exist.")
        send("Your configuration specifies the keystore to be in the following directory: $location")
        send("However in that directory there is no file 'passbird.ks'")
    }

    fun sendInputPath(fileDescription: String) {
        send("Please input an absolute path to a directory in which to create the '$fileDescription' file.")
    }

    fun sendCreateKeyStoreInformation() {
        userInterfaceAdapterPort.sendLineBreak()
        send("Your Passbird Keystore will be secured by a master password.")
        send("This master password gives access to all passwords stored in Passbird.")
        send("If you lose this password, you will not be able to access any passwords stored in Passbird.")
        send("Choose your master password wisely.")
        userInterfaceAdapterPort.sendLineBreak()
        send("You have to input your master password twice.")
        send("Your input will be hidden unless secure input is disabled in your configuration.")
        send("If your inputs do not match, you will have to repeat the procedure.")
        userInterfaceAdapterPort.sendLineBreak()
    }

    fun sendCreateKeyStoreSucceeded() { send("Keystore has been created successfully!") }
    fun sendNonMatchingInputs() { send("Your inputs do not match, please repeat.") }
    fun sendRestart() { send("Now restart Passbird to use it.") }
    fun sendGoodbye() { send("Goodbye!") }
    private fun send(message: String) = userInterfaceAdapterPort.send(outputOf(shellOf(message)))
}
