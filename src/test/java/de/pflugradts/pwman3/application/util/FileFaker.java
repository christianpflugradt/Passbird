package de.pflugradts.pwman3.application.util;

import java.io.File;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FileFaker {

    private File file;

    public static FileFaker faker() {
        return new FileFaker();
    }

    public FileFaker fakeFile() {
        file = mock(File.class);
        return this;
    }

    public FileFaker withDirectoryProperty(final boolean isDirectory) {
        given(file.isDirectory()).willReturn(isDirectory);
        return this;
    }

    public FileFaker withExistsProperty(final boolean exists) {
        given(file.exists()).willReturn(exists);
        return this;
    }

    public FileFaker withParentFile(final File parentFile) {
        given(file.getParentFile()).willReturn(parentFile);
        return this;
    }

    public File fake() {
        return file;
    }

}
