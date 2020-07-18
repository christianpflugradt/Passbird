package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.ListCommand;
import de.pflugradts.pwman3.application.util.ByteArrayUtils;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.PasswordService;
import java.util.List;
import java.util.stream.Collectors;

public class ListCommandHandler implements CommandHandler {

    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleListCommand(final ListCommand listCommand) {
        userInterfaceAdapterPort.send(Output.of(join(passwordService.findAllKeys().collect(Collectors.toList()))));
        userInterfaceAdapterPort.sendLineBreak();
    }

    private Bytes join(final List<Bytes> keyBytesList) {
        if (keyBytesList.isEmpty()) {
            return Bytes.of("database is empty");
        } else {
            final int count = keyBytesList.stream()
                    .map(Bytes::size)
                    .reduce((keyBytesList.size() - 1) * 2, Integer::sum);
            final byte[] bytes = new byte[count];
            int index = 0;
            for (final Bytes keyBytes : keyBytesList) {
                ByteArrayUtils.copyBytes(keyBytes, bytes, index);
                index += keyBytes.size();
                if (index < count) {
                    bytes[index++] = (byte) ',';
                    bytes[index++] = (byte) ' ';
                }
            }
            return Bytes.of(bytes);
        }
    }

}
