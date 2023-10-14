package de.pflugradts.passbird.application.commandhandling.command;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.commandhandling.command.base.Command;
import de.pflugradts.passbird.application.commandhandling.command.namespace.NamespaceCommandFactory;
import de.pflugradts.passbird.domain.model.transfer.Input;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class CommandFactory {

    @Inject
    private NamespaceCommandFactory namespaceCommandFactory;

    @SuppressWarnings("PMD.CyclomaticComplexity")
    public Command construct(final CommandType commandType, final Input input) {
        return switch (commandType) {
            case CUSTOM_SET -> new CustomSetCommand(input);
            case DISCARD -> new DiscardCommand(input);
            case EXPORT -> new ExportCommand(input);
            case GET -> new GetCommand(input);
            case HELP -> new HelpCommand();
            case IMPORT -> new ImportCommand(input);
            case LIST -> new ListCommand();
            case NAMESPACE -> namespaceCommandFactory.constructFromInput(input);
            case QUIT -> new QuitCommand();
            case RENAME -> new RenameCommand(input);
            case SET -> new SetCommand(input);
            case VIEW -> new ViewCommand(input);
            default -> new NullCommand();
        };
    }

}
