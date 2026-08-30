package de.pflugradts.passbird.application.configuration

import de.pflugradts.passbird.application.GlobalHotkeyBackend
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class ConfigurationYamlMapperTest {
    private val mapper = ConfigurationYamlMapper()
    private val fullConfigurationYaml = """
        application:
          backup:
            location: /vault/backups
            numberOfBackups: 7
            configuration:
              enabled: false
              location: /vault/config-backups
              numberOfBackups: 4
            passwordTree:
              enabled: false
              location: /vault/tree-backups
              numberOfBackups: 3
            keyStore:
              enabled: true
              location: /vault/key-backups
              numberOfBackups: 2
          exchange:
            promptOnExportFile: false
          flow:
            loginProteinSlot: 1
            globalHotkey:
              enabled: false
              key: x
              backend: x11
          inactivityLimit:
            enabled: true
            limitInMinutes: 15
          password:
            length: 32
            specialCharacters: false
            promptOnRemoval: false
            customPasswordConfigurations:
              - name: compact
                length: 12
                hasNumbers: false
                hasLowercaseLetters: true
                hasUppercaseLetters: false
                hasSpecialCharacters: true
                unusedSpecialCharacters: "!?"
          trash:
            retentionDays: 45
          yolk:
            algorithm: SHA256
            copyToClipboard: false
            digits: 8
            periodSeconds: 60
        adapter:
          clipboard:
            nativeTooling:
              enabled: false
            reset:
              enabled: false
              delaySeconds: 25
          keyStore:
            location: /vault/keys
          passwordTree:
            location: /vault/tree
            verifySignature: false
            verifyChecksum: false
          userInterface:
            ansiEscapeCodes:
              enabled: true
            audibleBell: true
            secureInput: false
        domain:
          eggIdMemory:
            enabled: false
            persisted: true
            updateOnFavoriteUse: false
          protein:
            secureProteinStructureInput: false
            promptForProteinStructureInputToggle: true
            templates:
              - name: login
                0: domain
                1: username
    """.trimIndent()

    @Test
    fun `should parse nested configuration and protein template slots`() {
        val actual = mapper.readConfiguration(
            """
            application:
              password:
                length: 24
              yolk:
                algorithm: SHA256
                digits: 8
            domain:
              protein:
                templates:
                  - name: web-login
                    0: domain
                    4: description
            """.trimIndent().byteInputStream(),
        )

        expectThat(actual.application.password.length) isEqualTo 24
        expectThat(actual.application.yolk.algorithm) isEqualTo "SHA256"
        expectThat(actual.application.yolk.digits) isEqualTo 8
        expectThat(actual.application.flow.loginProteinSlot) isEqualTo 0
        expectThat(actual.application.flow.globalHotkey.enabled).isFalse()
        expectThat(actual.application.flow.globalHotkey.key) isEqualTo "P"
        expectThat(actual.application.flow.globalHotkey.backend) isEqualTo GlobalHotkeyBackend.AUTO
        expectThat(actual.domain.protein.templates).hasSize(1)
        expectThat(actual.domain.protein.templates.first().slots) isEqualTo mapOf(0 to "domain", 4 to "description")
    }

    @Test
    fun `should accept carbon global hotkey backend`() {
        val actual = mapper.readConfiguration(
            """
            application:
              flow:
                globalHotkey:
                  backend: carbon
            """.trimIndent().byteInputStream(),
        )

        expectThat(actual.application.flow.globalHotkey.backend).isEqualTo(GlobalHotkeyBackend.CARBON)
    }

    @Test
    fun `should reject unrecognized configuration properties`() {
        val actual = kotlin.runCatching {
            mapper.readConfiguration(
                """
                application:
                  unsupported: true
                """.trimIndent().byteInputStream(),
            )
        }

        expectThat(actual.isFailure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull()
        expectThat(actual.exceptionOrNull()!!.message!!).contains("Unrecognized field \"application.unsupported\"")
    }

    @Test
    fun `should write configuration that can be loaded again`() {
        val configuration = Configuration(
            application = Configuration.Application(
                backup = Configuration.Backup(location = "/tmp/backups"),
                flow = Configuration.Flow(
                    loginProteinSlot = 2,
                    globalHotkey = Configuration.GlobalHotkey(enabled = false, key = "z", backend = GlobalHotkeyBackend.QUARTZ),
                ),
            ),
            adapter = Configuration.Adapter(
                keyStore = Configuration.KeyStore(location = "/tmp"),
                passwordTree = Configuration.PasswordTree(location = "/tree"),
            ),
            domain = Configuration.Domain(
                protein = Configuration.Protein(
                    templates = listOf(
                        Configuration.ProteinTemplate(name = "web-login").apply {
                            putSlot("0", "domain")
                            putSlot("1", "user")
                        },
                    ),
                ),
            ),
        )

        val yaml = mapper.writeConfiguration(configuration)
        val reloaded = mapper.readConfiguration(yaml.byteInputStream())

        expectThat(yaml).contains("templates:")
        expectThat(yaml).contains("name: web-login")
        expectThat(reloaded.application.flow.loginProteinSlot) isEqualTo 2
        expectThat(reloaded.application.flow.globalHotkey.enabled).isFalse()
        expectThat(reloaded.application.flow.globalHotkey.key) isEqualTo "z"
        expectThat(reloaded.application.flow.globalHotkey.backend) isEqualTo GlobalHotkeyBackend.QUARTZ
        expectThat(reloaded.adapter.keyStore.location) isEqualTo "/tmp"
        expectThat(reloaded.adapter.passwordTree.location) isEqualTo "/tree"
        expectThat(reloaded.domain.protein.templates.first().slots) isEqualTo mapOf(0 to "domain", 1 to "user")
    }

    @Test
    fun `should parse all supported configuration sections with explicit values`() {
        val actual = mapper.readConfiguration(fullConfigurationYaml.byteInputStream())

        assertExplicitApplicationConfiguration(actual)
        assertExplicitAdapterConfiguration(actual)
        assertExplicitDomainConfiguration(actual)
    }

    @Test
    fun `should support null backup override values`() {
        val actual = mapper.readConfiguration(
            """
            application:
              backup:
                configuration:
                  enabled: false
                  location: null
                  numberOfBackups: null
            """.trimIndent().byteInputStream(),
        )

        expectThat(actual.application.backup.configuration.enabled).isFalse()
        expectThat(actual.application.backup.configuration.location) isEqualTo null
        expectThat(actual.application.backup.configuration.numberOfBackups) isEqualTo null
    }

    @Test
    fun `should apply defaults inside present nested sections`() {
        val actual = mapper.readConfiguration(
            """
            application:
              backup:
                configuration: {}
              exchange: {}
              flow:
                globalHotkey: {}
              inactivityLimit: {}
              password:
                customPasswordConfigurations:
                  - {}
            adapter:
              clipboard:
                nativeTooling: {}
                reset: {}
              keyStore: {}
              passwordTree: {}
              userInterface:
                ansiEscapeCodes: {}
            domain:
              eggIdMemory: {}
              protein:
                templates:
                  - name: login
            """.trimIndent().byteInputStream(),
        )

        assertDefaultsInsidePresentApplicationSections(actual)
        assertDefaultsInsidePresentAdapterSections(actual)
        assertDefaultsInsidePresentDomainSections(actual)
    }

    @Test
    fun `should apply defaults for omitted child sections when parent sections are present`() {
        val actual = mapper.readConfiguration(
            """
            application: {}
            adapter: {}
            domain:
              protein: {}
            """.trimIndent().byteInputStream(),
        )

        expectThat(actual.application.backup.location) isEqualTo "backups"
        expectThat(actual.application.exchange.promptOnExportFile).isTrue()
        expectThat(actual.application.flow.loginProteinSlot) isEqualTo 0
        expectThat(actual.application.flow.globalHotkey.enabled).isFalse()
        expectThat(actual.application.flow.globalHotkey.key) isEqualTo "P"
        expectThat(actual.application.flow.globalHotkey.backend) isEqualTo GlobalHotkeyBackend.AUTO
        expectThat(actual.application.inactivityLimit.limitInMinutes) isEqualTo 10
        expectThat(actual.application.trash.retentionDays) isEqualTo 365
        expectThat(actual.application.password.length) isEqualTo 20
        expectThat(actual.application.yolk.algorithm) isEqualTo "SHA1"
        expectThat(actual.adapter.clipboard.nativeTooling.enabled).isTrue()
        expectThat(actual.adapter.keyStore.location) isEqualTo ""
        expectThat(actual.adapter.passwordTree.verifyChecksum).isTrue()
        expectThat(actual.adapter.userInterface.secureInput).isTrue()
        expectThat(actual.domain.eggIdMemory.enabled).isTrue()
        expectThat(actual.domain.protein.secureProteinStructureInput).isTrue()
        expectThat(actual.domain.protein.promptForProteinStructureInputToggle).isFalse()
        expectThat(actual.domain.protein.templates) isEqualTo emptyList()
    }

    @Test
    fun `should reject configurations with a non mapping root`() {
        val actual = kotlin.runCatching {
            mapper.readConfiguration(
                """
                - application
                """.trimIndent().byteInputStream(),
            )
        }

        expectThat(actual.isFailure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull()
        expectThat(actual.exceptionOrNull()!!.message!!).contains("Configuration must be a YAML mapping")
    }

    @Test
    fun `should reject invalid scalar and list item types`() {
        assertInvalidConfiguration(
            """
            application:
              password:
                length: invalid
            """.trimIndent(),
            "configuration.application.password.length must be an integer",
        )
        assertInvalidConfiguration(
            """
            domain:
              protein:
                templates:
                  - name: login
                    0: 123
            """.trimIndent(),
            "Protein template 'login' contains invalid slot",
        )
        assertInvalidConfiguration(
            """
            application:
              password:
                customPasswordConfigurations:
                  - null
            """.trimIndent(),
            "configuration.application.password.customPasswordConfigurations[0] must not be null",
        )
        assertInvalidConfiguration(
            """
            application:
              flow:
                globalHotkey:
                  key: "12"
            """.trimIndent(),
            "Global hotkey key is invalid",
        )
        assertInvalidConfiguration(
            """
            application:
              flow:
                globalHotkey:
                  backend: cocoa
            """.trimIndent(),
            "Global hotkey backend is invalid",
        )
        assertInvalidConfiguration(
            """
            application:
              flow:
                loginProteinSlot: 11
            """.trimIndent(),
            "Login Protein Slot is invalid",
        )
    }

    private fun assertDefaultsInsidePresentApplicationSections(configuration: Configuration) {
        expectThat(configuration.application.backup.configuration.enabled).isTrue()
        expectThat(configuration.application.backup.configuration.location) isEqualTo null
        expectThat(configuration.application.backup.configuration.numberOfBackups) isEqualTo null
        expectThat(configuration.application.exchange.promptOnExportFile).isTrue()
        expectThat(configuration.application.flow.loginProteinSlot) isEqualTo 0
        expectThat(configuration.application.flow.globalHotkey.enabled).isFalse()
        expectThat(configuration.application.flow.globalHotkey.key) isEqualTo "P"
        expectThat(configuration.application.flow.globalHotkey.backend) isEqualTo GlobalHotkeyBackend.AUTO
        expectThat(configuration.application.inactivityLimit.enabled).isFalse()
        expectThat(configuration.application.inactivityLimit.limitInMinutes) isEqualTo 10
        expectThat(configuration.application.trash.retentionDays) isEqualTo 365
        expectThat(configuration.application.password.customPasswordConfigurations).hasSize(1)
        expectThat(configuration.application.password.customPasswordConfigurations.first().name) isEqualTo ""
        expectThat(configuration.application.password.customPasswordConfigurations.first().length) isEqualTo 20
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasNumbers).isTrue()
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasLowercaseLetters).isTrue()
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasUppercaseLetters).isTrue()
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasSpecialCharacters).isTrue()
        expectThat(configuration.application.password.customPasswordConfigurations.first().unusedSpecialCharacters) isEqualTo ""
    }

    private fun assertDefaultsInsidePresentAdapterSections(configuration: Configuration) {
        expectThat(configuration.adapter.clipboard.nativeTooling.enabled).isTrue()
        expectThat(configuration.adapter.clipboard.reset.enabled).isTrue()
        expectThat(configuration.adapter.clipboard.reset.delaySeconds) isEqualTo 10
        expectThat(configuration.adapter.keyStore.location) isEqualTo ""
        expectThat(configuration.adapter.passwordTree.location) isEqualTo ""
        expectThat(configuration.adapter.passwordTree.verifySignature).isTrue()
        expectThat(configuration.adapter.passwordTree.verifyChecksum).isTrue()
        expectThat(configuration.adapter.userInterface.ansiEscapeCodes.enabled).isFalse()
        expectThat(configuration.adapter.userInterface.audibleBell).isFalse()
        expectThat(configuration.adapter.userInterface.secureInput).isTrue()
    }

    private fun assertDefaultsInsidePresentDomainSections(configuration: Configuration) {
        expectThat(configuration.domain.eggIdMemory.enabled).isTrue()
        expectThat(configuration.domain.eggIdMemory.persisted).isFalse()
        expectThat(configuration.domain.eggIdMemory.updateOnFavoriteUse).isTrue()
        expectThat(configuration.domain.protein.templates).hasSize(1)
        expectThat(configuration.domain.protein.templates.first().name) isEqualTo "login"
        expectThat(configuration.domain.protein.templates.first().slots) isEqualTo emptyMap()
    }

    private fun assertInvalidConfiguration(yaml: String, message: String) {
        val actual = kotlin.runCatching { mapper.readConfiguration(yaml.byteInputStream()) }

        expectThat(actual.isFailure).isTrue()
        expectThat(actual.exceptionOrNull()).isNotNull()
        expectThat(actual.exceptionOrNull()!!.message!!).contains(message)
    }

    private fun assertExplicitApplicationConfiguration(configuration: Configuration) {
        expectThat(configuration.application.backup.location) isEqualTo "/vault/backups"
        expectThat(configuration.application.backup.numberOfBackups) isEqualTo 7
        expectThat(configuration.application.backup.configuration.enabled).isFalse()
        expectThat(configuration.application.backup.configuration.location) isEqualTo "/vault/config-backups"
        expectThat(configuration.application.backup.configuration.numberOfBackups) isEqualTo 4
        expectThat(configuration.application.exchange.promptOnExportFile).isFalse()
        expectThat(configuration.application.flow.loginProteinSlot) isEqualTo 1
        expectThat(configuration.application.flow.globalHotkey.enabled).isFalse()
        expectThat(configuration.application.flow.globalHotkey.key) isEqualTo "x"
        expectThat(configuration.application.flow.globalHotkey.backend) isEqualTo GlobalHotkeyBackend.X11
        expectThat(configuration.application.inactivityLimit.enabled).isTrue()
        expectThat(configuration.application.inactivityLimit.limitInMinutes) isEqualTo 15
        expectThat(configuration.application.trash.retentionDays) isEqualTo 45
        expectThat(configuration.application.password.length) isEqualTo 32
        expectThat(configuration.application.password.specialCharacters).isFalse()
        expectThat(configuration.application.password.promptOnRemoval).isFalse()
        expectThat(configuration.application.password.customPasswordConfigurations).hasSize(1)
        expectThat(configuration.application.password.customPasswordConfigurations.first().name) isEqualTo "compact"
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasNumbers).isFalse()
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasLowercaseLetters).isTrue()
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasUppercaseLetters).isFalse()
        expectThat(configuration.application.password.customPasswordConfigurations.first().hasSpecialCharacters).isTrue()
        expectThat(configuration.application.password.customPasswordConfigurations.first().unusedSpecialCharacters) isEqualTo "!?"
        expectThat(configuration.application.yolk.algorithm) isEqualTo "SHA256"
        expectThat(configuration.application.yolk.copyToClipboard).isFalse()
        expectThat(configuration.application.yolk.digits) isEqualTo 8
        expectThat(configuration.application.yolk.periodSeconds) isEqualTo 60
    }

    private fun assertExplicitAdapterConfiguration(configuration: Configuration) {
        expectThat(configuration.adapter.clipboard.nativeTooling.enabled).isFalse()
        expectThat(configuration.adapter.clipboard.reset.enabled).isFalse()
        expectThat(configuration.adapter.clipboard.reset.delaySeconds) isEqualTo 25
        expectThat(configuration.adapter.keyStore.location) isEqualTo "/vault/keys"
        expectThat(configuration.adapter.passwordTree.location) isEqualTo "/vault/tree"
        expectThat(configuration.adapter.passwordTree.verifySignature).isFalse()
        expectThat(configuration.adapter.passwordTree.verifyChecksum).isFalse()
        expectThat(configuration.adapter.userInterface.ansiEscapeCodes.enabled).isTrue()
        expectThat(configuration.adapter.userInterface.audibleBell).isTrue()
        expectThat(configuration.adapter.userInterface.secureInput).isFalse()
    }

    private fun assertExplicitDomainConfiguration(configuration: Configuration) {
        expectThat(configuration.domain.eggIdMemory.enabled).isFalse()
        expectThat(configuration.domain.eggIdMemory.persisted).isTrue()
        expectThat(configuration.domain.eggIdMemory.updateOnFavoriteUse).isFalse()
        expectThat(configuration.domain.protein.secureProteinStructureInput).isFalse()
        expectThat(configuration.domain.protein.promptForProteinStructureInputToggle).isTrue()
        expectThat(configuration.domain.protein.templates.first().slots) isEqualTo mapOf(0 to "domain", 1 to "username")
    }
}
