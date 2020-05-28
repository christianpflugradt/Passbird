package de.pflugradts.pwman3.application.exchange;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.PasswordService;
import io.vavr.Tuple2;

public class PasswordImportExportService implements ImportExportService {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ExchangeFactory exchangeFactory;
    @Inject
    private PasswordService passwordService;

    @Override
    public void imp(final String uri) {
        exchangeFactory.createPasswordExchange(uri)
                .receive()
                .onFailure(failureCollector::collectImportFailure)
                .onSuccess(result -> result.forEach(
                    passwordEntry -> passwordService.putPasswordEntry(passwordEntry._1, passwordEntry._2)));
    }

    @Override
    public void exp(final String uri) {
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
