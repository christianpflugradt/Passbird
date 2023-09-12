package de.pflugradts.passbird.application.exchange;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.PasswordService;

import java.util.Optional;
import java.util.stream.Stream;

public class PasswordImportExportService implements ImportExportService {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ExchangeFactory exchangeFactory;
    @Inject
    private PasswordService passwordService;

    @Override
    public Stream<Bytes> peekImportKeyBytes(final String uri) {
        return exchangeFactory.createPasswordExchange(uri)
                .receive()
                // .onFailure(failureCollector::collectImportFailure)
                .map(Tuple::get_1);
    }

    @Override
    public void importPasswordEntries(final String uri) {
        passwordService.putPasswordEntries(exchangeFactory.createPasswordExchange(uri).receive());
    }

    @Override
    public void exportPasswordEntries(final String uri) {
        final var exportData = passwordService.findAllKeys()
                        .map(this::retrievePasswordEntryTuple).toList();
        if (!exportData.isEmpty()) {
            exchangeFactory.createPasswordExchange(uri)
                .send(exportData.stream().filter(Optional::isPresent).map(Optional::get));
        }
    }

    private Optional<Tuple<Bytes, Bytes>> retrievePasswordEntryTuple(final Bytes keyBytes) {
        return passwordService.viewPassword(keyBytes)
                .map(bytes -> new Tuple<>(keyBytes, bytes));
    }

}
