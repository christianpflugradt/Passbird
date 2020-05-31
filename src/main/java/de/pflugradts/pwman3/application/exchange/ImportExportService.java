package de.pflugradts.pwman3.application.exchange;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import java.util.stream.Stream;

public interface ImportExportService {
    Stream<Bytes> peekImportKeyBytes(String uri);
    void importPasswordEntries(String uri);
    void exportPasswordEntries(String uri);
}
