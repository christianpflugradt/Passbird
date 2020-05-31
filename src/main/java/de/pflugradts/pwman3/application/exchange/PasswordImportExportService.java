package de.pflugradts.pwman3.application.exchange;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.PasswordService;
import io.vavr.Tuple2;
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
                .onFailure(failureCollector::collectImportFailure)
                .getOrElse(Stream::empty)
                .map(Tuple2::_1);
    }

    @Override
    public void importPasswordEntries(final String uri) {
        exchangeFactory.createPasswordExchange(uri)
                .receive()
                .onFailure(failureCollector::collectImportFailure)
                .onSuccess(passwordService::putPasswordEntries);
    }

    @Override
    public void exportPasswordEntries(final String uri) {
        exchangeFactory.createPasswordExchange(uri).send(
                passwordService
                        .findAllKeys()
                        .map(this::retrievePasswordEntryTuple))
                .onFailure(failureCollector::collectExportFailure);
    }

    private Tuple2<Bytes, Bytes> retrievePasswordEntryTuple(final Bytes keyBytes) {
        return passwordService.viewPassword(keyBytes)
                .map(bytes -> new Tuple2<>(keyBytes, bytes))
                .orElse(null);
    }

}
