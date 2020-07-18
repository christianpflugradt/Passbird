package de.pflugradts.pwman3.application.exchange;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.PasswordService;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
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
        final var exportData = passwordService.findAllKeys()
                        .onFailure(failureCollector::collectPasswordEntriesFailure)
                        .getOrElse(Stream.empty())
                        .map(this::retrievePasswordEntryTuple).collect(Collectors.toList());
        if (!exportData.isEmpty()) {
            exportData.stream().filter(Either::isLeft).findAny().ifPresentOrElse(
                either -> failureCollector.collectPasswordEntriesFailure(either.getLeft()),
                () -> exchangeFactory.createPasswordExchange(uri)
                        .send(exportData.stream().map(Either::get))
                        .onFailure(failureCollector::collectExportFailure));
        }
    }

    private Either<Throwable, Tuple2<Bytes, Bytes>> retrievePasswordEntryTuple(final Bytes keyBytes) {
        return passwordService.viewPassword(keyBytes)
                .orElse(Try.failure(new NoSuchElementException()))
                .toEither()
                .map(bytes -> new Tuple2<>(keyBytes, bytes));
    }

}
