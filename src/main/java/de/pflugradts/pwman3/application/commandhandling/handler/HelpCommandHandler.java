package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.HelpCommand;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;

public class HelpCommandHandler implements CommandHandler {

    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleHelpCommand(final HelpCommand helpCommand) {
        userInterfaceAdapterPort.send(Output.of(Bytes.of(
                "Usage: [command][parameter]\n\n"
                        + "A command takes at most one parameter which is either\n"
                        + "a key to a password or an absolute path to a file.\n\n"
                        + "commands:\n"
                        + "\tg[key] (get) copies the password for that key to clipboard\n"
                        + "\ts[key] (set) sets a random password for a key overwriting any that existed\n"
                        + "\tc[key] (custom set) like set but prompts the user to input a new password\n"
                        + "\tv[key] (view) prints the password for that key to the console\n"
                        + "\td[key] (discard) removes key and password from the database\n"
                        + "\te[directory] (export) exports the password database as a human readable json file "
                        + "to the specified directory\n"
                        + "\ti[directory] (import) imports a json file containing passwords into the database "
                        + "from the specified directory\n"
                        + "\tl (list) nonparameterized, lists all keys in the database\n"
                        + "\th (help) nonparameterized, prints this help\n"
                        + "\tq (quit) quits pwman3 applicationImpl\n\n"
        )));
    }

}
