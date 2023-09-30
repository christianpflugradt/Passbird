package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;

public class HelpCommandHandler implements CommandHandler {

    @Inject
    private SystemOperation systemOperation;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleHelpCommand(final HelpCommand helpCommand) {
        printUsage();
    }

    private void printUsage() {
        userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(String.format(
                "%nUsage: [command][parameter]%n"
                        + "A command takes at most one parameter which is either%n"
                        + "a key to a password or an absolute path to a file.%n%n"
                        + "commands:%n"
                        + "\tg[key] (get) copies the password for that key to clipboard%n"
                        + "\ts[key] (set) sets a random password for a key overwriting any that existed%n"
                        + "\tc[key] (custom set) like set but prompts the user to input a new password%n"
                        + "\tv[key] (view) prints the password for that key to the console%n"
                        + "\tr[key] (rename) renames a key by prompting the user for a new one%n"
                        + "\td[key] (discard) removes key and password from the database%n"
                        + "\te[directory] (export) exports the password database as a human readable json file "
                        + "to the specified directory%n"
                        + "\ti[directory] (import) imports a json file containing passwords into the database "
                        + "from the specified directory%n"
                        + "\tl (list) non parameterized, lists all keys in the database%n"
                        + "\tn (namespaces) view available namespaces and print namespace specific help%n"
                        + "\th (help) non parameterized, prints this help%n"
                        + "\tq (quit) quits pwman3 applicationImpl%n%n"
        ))));
        userInterfaceAdapterPort.sendLineBreak();
    }

}
