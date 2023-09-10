package de.pflugradts.passbird.application.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PathFaker {

    private Path path;

    public static PathFaker faker() {
        return new PathFaker();
    }

    public PathFaker fakePath() {
        path = mock(Path.class);
        return this;
    }

    public PathFaker withFileResolvingToFilename(final File file, final String filename) {
        final var resolvedPath = mock(Path.class);
        given(resolvedPath.toFile()).willReturn(file);
        given(path.resolve(filename)).willReturn(resolvedPath);
        return this;
    }

    public PathFaker withFileRepresentation(final File fileRepresentation) {
        given(path.toFile()).willReturn(fileRepresentation);
        return this;
    }

    public Path fake() {
        return path;
    }

}
