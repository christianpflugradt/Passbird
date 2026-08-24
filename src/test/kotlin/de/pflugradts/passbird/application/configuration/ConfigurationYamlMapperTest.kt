package de.pflugradts.passbird.application.configuration

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class ConfigurationYamlMapperTest {
    private val mapper = ConfigurationYamlMapper()

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
        expectThat(actual.domain.protein.templates).hasSize(1)
        expectThat(actual.domain.protein.templates.first().slots) isEqualTo mapOf(0 to "domain", 4 to "description")
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
        expectThat(reloaded.adapter.keyStore.location) isEqualTo "/tmp"
        expectThat(reloaded.adapter.passwordTree.location) isEqualTo "/tree"
        expectThat(reloaded.domain.protein.templates.first().slots) isEqualTo mapOf(0 to "domain", 1 to "user")
    }
}
