package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.HelpCommand;
import de.pflugradts.pwman3.application.license.LicenseManager;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import java.io.File;
import java.nio.file.Path;
import static de.pflugradts.pwman3.application.license.LicenseManager.LICENSE_FILENAME;
import static de.pflugradts.pwman3.application.license.LicenseManager.THIRD_PARTY_LICENSES_FILENAME;

public class HelpCommandHandler implements CommandHandler {

    @Inject
    private SystemOperation systemOperation;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private LicenseManager licenseManager;

    @Subscribe
    private void handleHelpCommand(final HelpCommand helpCommand) {
        if (helpCommand.getArgument().equals(Bytes.of("license"))) {
            openLicense();
        } else if (helpCommand.getArgument().equals(Bytes.of("thirdparty"))) {
            openDependencyReport();
        } else {
            printUsage();
        }
    }

    private void printUsage() {
        userInterfaceAdapterPort.send(Output.of(Bytes.of(String.format(
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
                        + "\ttype 'hlicense' to view license for PwMan3%n"
                        + "\ttype 'hthirdparty' to view a list of 3rd party libraries and their licenses"
        ))));
        userInterfaceAdapterPort.sendLineBreak();
    }

    private void openLicense() {
        openResource(LICENSE_FILENAME, "If you are not seeing the license, "
                + "please open the following file with a text editor of your choice: %s");
    }

    private void openDependencyReport() {
        openResource(THIRD_PARTY_LICENSES_FILENAME, "If you are not seeing the 3rdparty licenses report, "
                        + "please open the following url in your web browser of choice: %s");
    }

    private void openResource(final String resource, final String messageTemplate) {
        final var resourceFile = new File(resource);
        licenseManager.verifyLicenseFilesExist();
        systemOperation.openFile(resourceFile);
        userInterfaceAdapterPort.send(Output.of(Bytes.of(String.format(
                messageTemplate,
                Path.of(System.getProperty("user.dir"), resource).toString()))));
        userInterfaceAdapterPort.sendLineBreak();
    }

}
