package de.pflugradts.passbird.application.security

class AesGcmCipherTest {

    // FIXME mock private function correctly and re-enable tests
    /*private val secretShell = shellOf(byteArrayOf(97, -87, -65, -105, -48, -75, 65, -72, 67, -25, -88, -123, -28, 42, -61, 39))
    private val staticIv: Shell = shellOf(byteArrayOf(42, -119, 67, -38, 5, 96, -74, 111, -123, 34, -8, 91))

    @Test
    fun `should encrypt shell`() {
        // given
        val cryptoProvider = spyk(AesGcmCipher(secretShell))
        val givenDecryptedShell = shellOf(byteArrayOf(91, 87, 99, 52, 97, 79, 120, 82, 35, 40, 59, 77, 61, 111, 111, 110, 67, 102, 89, 108))
        every { cryptoProvider invoke "requestSecureIv" } returns staticIv
        val expectedEncryptedShell = shellOf(
            byteArrayOf(
                -66, 46, -25, 56, -75, -7, -111, 127, -107, -85, 113, 115, -25, -49, 4, -78, -92, -14, -27,
                -19, -107, 49, -42, 39, 19, -43, 123, -125, 62, 40, 127, -2, 119, 13, 98, 29, -93, -115, 21, 14,
                -95, -85, 109, 88, -83, -88, 96, 2,
            ),
        )

        // when
        val actual = cryptoProvider.encrypt(givenDecryptedShell)

        // then
        expectThat(actual) isEqualTo expectedEncryptedShell.fakeEnc()
    }

    @Test
    fun `should decrypt shell`() {
        // given
        val cryptoProvider = AesGcmCipher(secretShell)
        val givenEncryptedShell = shellOf(
            byteArrayOf(
                -66, 46, -25, 56, -75, -7, -111, 127, -107, -85, 113, 115, -25, -49, 4, -78, -92, -14, -27,
                -19, -107, 49, -42, 39, 19, -43, 123, -125, 62, 40, 127, -2, 119, 13, 98, 29, -93, -115, 21, 14,
                -95, -85, 109, 88, -83, -88, 96, 2,
            ),
        )
        val expectedDecryptedShell =
            shellOf(byteArrayOf(91, 87, 99, 52, 97, 79, 120, 82, 35, 40, 59, 77, 61, 111, 111, 110, 67, 102, 89, 108))

        // when
        val actual = cryptoProvider.decrypt(givenEncryptedShell.fakeEnc())

        // then
        expectThat(actual) isEqualTo expectedDecryptedShell
    }*/
}
