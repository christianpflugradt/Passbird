package de.pflugradts.passbird.application.security

import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class CipherizerTest {

    private val secretBytes = bytesOf(byteArrayOf(97, -87, -65, -105, -48, -75, 65, -72, 67, -25, -88, -123, -28, 42, -61, 39))
    private val ivBytes = bytesOf(byteArrayOf(-93, -95, 112, -58, -16, -60, 90, 0, 60, 33, 24, -111, -40, 39, 87, -66))

    @Test
    fun shouldEncryptBytes() {
        // given
        val cryptoProvider = Cipherizer(secretBytes, ivBytes)
        val givenDecryptedBytes = bytesOf(byteArrayOf(91, 87, 99, 52, 97, 79, 120, 82, 35, 40, 59, 77, 61, 111, 111, 110, 67, 102, 89, 108))
        val expectedEncryptedBytes = bytesOf(
            byteArrayOf(
                -66, 46, -25, 56, -75, -7, -111, 127, -107, -85, 113, 115, -25, -49, 4, -78, -92, -14, -27,
                -19, -107, 49, -42, 39, 19, -43, 123, -125, 62, 40, 127, -2, 119, 13, 98, 29, -93, -115, 21, 14,
                -95, -85, 109, 88, -83, -88, 96, 2,
            ),
        )

        // when
        val actual = cryptoProvider.encrypt(givenDecryptedBytes)

        // then
        Assertions.assertThat(actual).isNotNull().isEqualTo(expectedEncryptedBytes)
    }

    @Test
    fun shouldDecryptBytes() {
        // given
        val cryptoProvider = Cipherizer(secretBytes, ivBytes)
        val givenEncryptedBytes = bytesOf(
            byteArrayOf(
                -66, 46, -25, 56, -75, -7, -111, 127, -107, -85, 113, 115, -25, -49, 4, -78, -92, -14, -27,
                -19, -107, 49, -42, 39, 19, -43, 123, -125, 62, 40, 127, -2, 119, 13, 98, 29, -93, -115, 21, 14,
                -95, -85, 109, 88, -83, -88, 96, 2,
            ),
        )
        val expectedDecryptedBytes =
            bytesOf(byteArrayOf(91, 87, 99, 52, 97, 79, 120, 82, 35, 40, 59, 77, 61, 111, 111, 110, 67, 102, 89, 108))

        // when
        val actual = cryptoProvider.decrypt(givenEncryptedBytes)

        // then
        Assertions.assertThat(actual).isNotNull().isEqualTo(expectedDecryptedBytes)
    }
}
