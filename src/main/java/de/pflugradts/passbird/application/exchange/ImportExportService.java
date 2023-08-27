package de.pflugradts.passbird.application.exchange;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import java.util.stream.Stream;

public interface ImportExportService {
    Stream<Bytes> peekImportKeyBytes(String uri);
    void importPasswordEntries(String uri);
    void exportPasswordEntries(String uri);
}
