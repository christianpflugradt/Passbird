package de.pflugradts.passbird.application.exchange;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.BytePair;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import kotlin.Pair;

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
                .map(BytePair::getValue)
                .map(Pair::getFirst);
    }

    @Override
    public void importPasswordEntries(final String uri) {
        passwordService.putPasswordEntries(exchangeFactory.createPasswordExchange(uri).receive().map(
            it -> new Tuple<>(it.getValue().getFirst(), it.getValue().getSecond()) // FIXME when migrated to kotlin
        ));
    }

    @Override
    public void exportPasswordEntries(final String uri) {
        final var exportData = passwordService.findAllKeys()
                        .map(this::retrievePasswordEntryTuple).toList();
        if (!exportData.isEmpty()) {
            exchangeFactory.createPasswordExchange(uri)
                .send_Deprecated(exportData.stream().filter(Optional::isPresent).map(Optional::get));
        }
    }

    private Optional<Tuple<Bytes, Bytes>> retrievePasswordEntryTuple(final Bytes keyBytes) {
        return passwordService.viewPassword(keyBytes)
                .map(bytes -> new Tuple<>(keyBytes, bytes));
    }

}
