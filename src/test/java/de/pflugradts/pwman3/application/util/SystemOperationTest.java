package de.pflugradts.pwman3.application.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class SystemOperationTest {

    private final SystemOperation systemOperation = mock(SystemOperation.class);

    @Test
    void shouldOpenHtmlFile() throws IOException, URISyntaxException {
        // given
        final var uri = new URI("test");
        final var givenFile = FileFaker.faker()
                .fakeFile()
                .withName("test.html")
                .withUri(uri).fake();
        final var desktop = mock(Desktop.class);
        given(systemOperation.getDesktop()).willReturn(desktop);
        given(systemOperation.openFile(givenFile)).willCallRealMethod();

        // when
        systemOperation.openFile(givenFile);

        // then
        then(desktop).should().browse(uri);
    }

    @Test
    void shouldOpenNonHtmlFile() throws IOException, URISyntaxException {
        // given
        final var uri = new URI("test");
        final var givenFile = FileFaker.faker()
                .fakeFile()
                .withName("test.txt").fake();
        final var desktop = mock(Desktop.class);
        given(systemOperation.getDesktop()).willReturn(desktop);
        given(systemOperation.openFile(givenFile)).willCallRealMethod();

        // when
        systemOperation.openFile(givenFile);

        // then
        then(desktop).should().open(givenFile);
    }

}
