package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.ListCommand;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;

import java.util.List;

import static de.pflugradts.passbird.application.util.ByteArrayUtilsKt.copyBytes;

public class ListCommandHandler implements CommandHandler {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleListCommand(final ListCommand listCommand) {
        userInterfaceAdapterPort.send(Output.Companion.outputOf(join(passwordService.findAllKeys().toList())));
        userInterfaceAdapterPort.sendLineBreak();
    }

    private Bytes join(final List<Bytes> keyBytesList) {
        if (keyBytesList.isEmpty()) {
            return Bytes.bytesOf("database is empty");
        } else {
            final int count = keyBytesList.stream()
                    .map(Bytes::getSize)
                    .reduce((keyBytesList.size() - 1) * 2, Integer::sum);
            final byte[] bytes = new byte[count];
            int index = 0;
            for (final Bytes keyBytes : keyBytesList) {
                copyBytes(keyBytes.toByteArray(), bytes, index, keyBytes.getSize());
                index += keyBytes.getSize();
                if (index < count) {
                    bytes[index++] = (byte) ',';
                    bytes[index++] = (byte) ' ';
                }
            }
            return Bytes.bytesOf(bytes);
        }
    }

}
