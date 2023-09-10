package de.pflugradts.passbird.application.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemOperationFaker {

    private SystemOperation systemOperation;
    private final Map<String, Path> paths = new HashMap<>();

    public static SystemOperationFaker faker() {
        return new SystemOperationFaker();
    }

    public SystemOperationFaker fakeSystemOperation() throws IOException {
        this.systemOperation = mock(SystemOperation.class);
        lenient().when(systemOperation.newInputStream(any())).thenReturn(mock(InputStream.class));
        lenient().when(systemOperation.newOutputStream(any())).thenReturn(mock(OutputStream.class));
        return this;
    }

    public SystemOperationFaker forInstance(final SystemOperation systemOperation) {
        this.systemOperation = systemOperation;
        return this;
    }

    public SystemOperationFaker withPath(final String uri, final Path path) {
        this.paths.put(uri, path);
        return this;
    }

    public SystemOperationFaker withConsoleDisabled() {
        given(systemOperation.isConsoleAvailable()).willReturn(false);
        return this;
    }

    public SystemOperationFaker withPasswordFromConsole(char[] password) {
        given(systemOperation.isConsoleAvailable()).willReturn(true);
        given(systemOperation.readPasswordFromConsole()).willReturn(password);
        return this;
    }

    public SystemOperation fake() {
        paths.forEach((uri, path) -> given(systemOperation.getPath(uri)).willReturn(path));
        return systemOperation;
    }

}
