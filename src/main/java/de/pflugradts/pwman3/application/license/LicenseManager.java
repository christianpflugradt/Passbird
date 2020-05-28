package de.pflugradts.pwman3.application.license;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import java.io.File;
import java.util.List;

public class LicenseManager {

    public static final String LICENSE_FILENAME = "pwman3-license.txt";
    public static final String THIRD_PARTY_LICENSES_FILENAME = "pwman3-licenses-3rdparty.html";

    @Inject
    private SystemOperation systemOperation;

    public void verifyLicenseFilesExist() {
        List.of(LICENSE_FILENAME, THIRD_PARTY_LICENSES_FILENAME).forEach(this::exportLicenseFile);
    }

    private void exportLicenseFile(final String licenseFileName) {
        final var licenseFile = new File(licenseFileName);
        if (!licenseFile.exists()) {
            systemOperation.writeBytesToFile(
                    systemOperation.getPath(licenseFile).getOrNull(),
                    systemOperation.getResourceAsBytes(licenseFileName).getOrElse(Bytes.empty()));
        }
    }

}
