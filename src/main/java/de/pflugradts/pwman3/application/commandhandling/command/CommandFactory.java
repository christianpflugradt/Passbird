package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Input;

@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class CommandFactory {

    @SuppressWarnings("PMD.CyclomaticComplexity")
    public Command construct(final CommandType commandType, final Input input) {
        switch (commandType) {
            case CUSTOM_SET: return new CustomSetCommand(input);
            case DISCARD: return new DiscardCommand(input);
            case EXPORT: return new ExportCommand(input);
            case GET: return new GetCommand(input);
            case HELP: return new HelpCommand(input);
            case IMPORT: return new ImportCommand(input);
            case LIST: return new ListCommand();
            case QUIT: return new QuitCommand();
            case SET: return new SetCommand(input);
            case VIEW: return new ViewCommand(input);
            default: return new NullCommand();
        }
    }

}
