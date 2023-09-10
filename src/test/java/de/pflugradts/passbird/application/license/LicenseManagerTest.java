package de.pflugradts.passbird.application.license;

import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static de.pflugradts.passbird.application.license.LicenseManager.LICENSE_FILENAME;
import static de.pflugradts.passbird.application.license.LicenseManager.THIRD_PARTY_LICENSES_FILENAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LicenseManagerTest {

    @Mock
    private SystemOperation systemOperation;
    @InjectMocks
    private LicenseManager licenseManager;

    @Test
    void shouldVerifyLicenseFilesExist() {
        // given
        final var licenseFileName = LICENSE_FILENAME;
        final var licenseBytes = mock(Bytes.class);
        final var licensePath = mock(Path.class);
        given(systemOperation.getResourceAsBytes(licenseFileName)).willReturn(Try.success(licenseBytes));
        given(systemOperation.getPath(eq(new File(licenseFileName)))).willReturn(licensePath);

        final var thirdPartyFileName = THIRD_PARTY_LICENSES_FILENAME;
        final var thirdPartyBytes = mock(Bytes.class);
        final var thirdPartyPath = mock(Path.class);
        given(systemOperation.getResourceAsBytes(thirdPartyFileName)).willReturn(Try.success(thirdPartyBytes));
        given(systemOperation.getPath(eq(new File(thirdPartyFileName)))).willReturn(thirdPartyPath);

        // when
        licenseManager.verifyLicenseFilesExist();

        // then
        then(systemOperation).should().writeBytesToFile(licensePath, licenseBytes);
        then(systemOperation).should().writeBytesToFile(thirdPartyPath, thirdPartyBytes);
    }

}
