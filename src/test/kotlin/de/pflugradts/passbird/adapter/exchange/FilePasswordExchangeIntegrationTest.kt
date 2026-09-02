package de.pflugradts.passbird.adapter.exchange

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.application.PasswordYolkInfo
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Files
import java.nio.file.StandardOpenOption

@Tag(INTEGRATION)
class FilePasswordExchangeIntegrationTest {
    private val home = Files.createTempDirectory("passbird-exchange")
    private val password = "correct horse battery staple".toCharArray()
    private val exchange = FilePasswordExchange(SystemOperation(), PassbirdRunContext(home.toString().toDirectory(), Slot.DEFAULT))
    private val exchangeFile = home.resolve(ReadableConfiguration.EXCHANGE_FILENAME)

    @AfterEach
    fun cleanup() {
        home.toFile().deleteRecursively()
    }

    @Test
    fun exportsAesEncryptedCsvZipAndRestoresData() {
        val info = PasswordInfo(
            ShellPair(shellOf("email"), shellOf("SecretPassword")),
            List(Slot.entries.size) {
                if (it ==
                    Slot.S2.index()
                ) {
                    ShellPair(shellOf("username"), shellOf("alice"))
                } else {
                    ShellPair(emptyShell(), emptyShell())
                }
            },
            PasswordYolkInfo(shellOf("JBSWY3DPEHPK3PXP"), "SHA256", 8, 45),
        )
        val workInfo = info.copy(
            first = ShellPair(shellOf("email"), shellOf("WorkPassword")),
            yolk = PasswordYolkInfo(shellOf("KRUGS4ZANFZSAYJA"), "SHA1", 6, 30),
        )
        val data = mapOf(
            createNest(shellOf("personal"), Slot.DEFAULT) to listOf(info),
            createNest(shellOf("work"), Slot.S2) to listOf(workInfo),
        )

        expectThat(exchange.send(data, password).failure).isFalse()

        val zip = ZipFile(exchangeFile.toFile(), password)
        expectThat(zip.isEncrypted).isTrue()
        expectThat(zip.fileHeaders.map { it.fileName }).contains("manifest.txt", "nests.csv", "eggs.csv", "proteins.csv", "yolks.csv")
        expectThat(zip.fileHeaders.map { it.encryptionMethod }.toSet()) isEqualTo setOf(EncryptionMethod.AES)
        expectThat(home.toFile().list()?.toSet()) isEqualTo setOf(ReadableConfiguration.EXCHANGE_FILENAME)
        expectThat(exchange.receive(password).getOrNull()) isEqualTo data
    }

    @Test
    fun rejectsIncorrectZipPassword() {
        exchange.send(emptyMap(), password)

        expectThat(exchange.receive("wrong password".toCharArray()).failure).isTrue()
    }

    @Test
    fun rejectsArchivesWithInvalidStructure() {
        listOf(
            baseEntries() - "manifest.txt",
            baseEntries() + ("manifest.txt" to "format=unsupported\n"),
            baseEntries() - "nests.csv",
            baseEntries() + ("nests.csv" to "wrong,header\n"),
            baseEntries() + ("nests.csv" to "nest_slot,nest_name\n0,home\n0,work\n"),
            baseEntries() + ("nests.csv" to "nest_slot,nest_name\n10,home\n"),
            baseEntries() + ("eggs.csv" to "egg_id,nest_slot,password\n,0,password\n"),
            baseEntries() + ("eggs.csv" to "egg_id,nest_slot,password\nemail,1,password\n"),
            baseEntries() + ("eggs.csv" to "egg_id,nest_slot,password\nemail,0,password\nemail,0,other\n"),
            baseEntries() + ("proteins.csv" to "egg_id,nest_slot,slot,type,structure\nmissing,0,0,type,structure\n"),
            baseEntries() + ("proteins.csv" to "egg_id,nest_slot,slot,type,structure\nemail,0,0,type,\n"),
            baseEntries() + ("proteins.csv" to "egg_id,nest_slot,slot,type,structure\nemail,0,0,type,structure\nemail,0,0,other,value\n"),
            baseEntries() + ("proteins.csv" to "egg_id,nest_slot,slot,type,structure\nemail,0,10,type,structure\n"),
            baseEntries() + ("yolks.csv" to "egg_id,nest_slot,secret,algorithm,digits,period\nmissing,0,secret,SHA1,6,30\n"),
            baseEntries() +
                ("yolks.csv" to "egg_id,nest_slot,secret,algorithm,digits,period\nemail,0,secret,SHA1,6,30\nemail,0,other,SHA1,6,30\n"),
            baseEntries() + ("yolks.csv" to "egg_id,nest_slot,secret,algorithm,digits,period\nemail,0,,SHA1,6,30\n"),
        ).forEach { entries ->
            writeArchive(entries.entries.map { it.toPair() })

            expectThat(exchange.receive(password).failure).isTrue()
        }
    }

    @Test
    fun rejectsDuplicateEntriesAndUnterminatedCsvFields() {
        writeArchive(baseEntries().entries.map { it.toPair() } + ("nests.csv" to "nest_slot,nest_name\n0,home\n"))

        expectThat(exchange.receive(password).failure).isTrue()

        writeArchive(
            baseEntries().entries.map { it.toPair() }.map { (name, content) ->
                if (name == "nests.csv") name to "nest_slot,nest_name\n\"0\",\"home" else name to content
            },
        )

        expectThat(exchange.receive(password).failure).isTrue()
    }

    @Test
    fun parsesQuotedCsvFieldsWithEscapedQuotesAndCarriageReturns() {
        writeArchive(
            baseEntries().entries.map { (name, content) ->
                if (name == "eggs.csv") name to "email,0,\"password \"\"quoted\"\"\"\r\n" else name to content
            },
        )

        expectThat(exchange.receive(password).failure).isTrue()
    }

    private fun baseEntries() = linkedMapOf(
        "manifest.txt" to "format=passbird-export\nversion=1\nencryption=zip-aes-256\nfiles=nests.csv,eggs.csv,proteins.csv,yolks.csv\n",
        "nests.csv" to "nest_slot,nest_name\n0,home\n",
        "eggs.csv" to "egg_id,nest_slot,password\nemail,0,password\n",
        "proteins.csv" to "egg_id,nest_slot,slot,type,structure\n",
        "yolks.csv" to "egg_id,nest_slot,secret,algorithm,digits,period\n",
    )

    private fun writeArchive(entries: List<Pair<String, String>>) {
        ZipOutputStream(
            Files.newOutputStream(exchangeFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
            password,
        ).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(
                    ZipParameters().apply {
                        fileNameInZip = name
                        isEncryptFiles = true
                        encryptionMethod = EncryptionMethod.AES
                        aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    },
                )
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }
}
