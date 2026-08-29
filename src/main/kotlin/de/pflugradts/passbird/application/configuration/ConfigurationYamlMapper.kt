package de.pflugradts.passbird.application.configuration

import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.common.FlowStyle
import java.io.InputStream

class ConfigurationYamlMapper {
    private val load = Load(LoadSettings.builder().build())
    private val dump = Dump(
        DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .build(),
    )

    fun readConfiguration(inputStream: InputStream): Configuration = inputStream.use { stream ->
        (load.loadFromInputStream(stream) ?: throw IllegalArgumentException("Configuration is empty"))
            .toNode("configuration")
            .toConfiguration()
            .validate()
    }

    fun writeConfiguration(configuration: ReadableConfiguration): String = dump.dumpToString(configuration.toYamlValue())
}

class UnrecognizedConfigurationPropertyException(propertyPath: String) :
    IllegalArgumentException("Unrecognized field \"$propertyPath\"")

private fun Any.toNode(path: String): ConfigurationNode = when (this) {
    is Map<*, *> -> ConfigurationNode(
        entries.associate { (key, value) ->
            (key?.toString() ?: throw IllegalArgumentException("Configuration contains invalid property name at $path")) to value
        }.toMutableMap(),
        path,
    )

    else -> throw IllegalArgumentException("Configuration must be a YAML mapping")
}

private fun ConfigurationNode.toConfiguration() = Configuration(
    application = nested("application")?.toApplication() ?: Configuration.Application(),
    adapter = nested("adapter")?.toAdapter() ?: Configuration.Adapter(),
    domain = nested("domain")?.toDomain() ?: Configuration.Domain(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toApplication() = Configuration.Application(
    backup = nested("backup")?.toBackup() ?: Configuration.Backup(),
    exchange = nested("exchange")?.toExchange() ?: Configuration.Exchange(),
    flow = nested("flow")?.toFlow() ?: Configuration.Flow(),
    inactivityLimit = nested("inactivityLimit")?.toInactivityLimit() ?: Configuration.InactivityLimit(),
    password = nested("password")?.toPassword() ?: Configuration.Password(),
    trash = nested("trash")?.toTrash() ?: Configuration.Trash(),
    yolk = nested("yolk")?.toYolk() ?: Configuration.Yolk(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toBackup() = Configuration.Backup(
    location = string("location") ?: Configuration.Backup().location,
    numberOfBackups = int("numberOfBackups") ?: Configuration.Backup().numberOfBackups,
    configuration = nested("configuration")?.toBackupSettings() ?: Configuration.BackupSettings(),
    passwordTree = nested("passwordTree")?.toBackupSettings() ?: Configuration.BackupSettings(),
    keyStore = nested("keyStore")?.toBackupSettings() ?: Configuration.BackupSettings(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toBackupSettings() = Configuration.BackupSettings(
    enabled = boolean("enabled") ?: Configuration.BackupSettings().enabled,
    location = nullableString("location"),
    numberOfBackups = nullableInt("numberOfBackups"),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toExchange() = Configuration.Exchange(
    promptOnExportFile = boolean("promptOnExportFile") ?: Configuration.Exchange().promptOnExportFile,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toFlow() = Configuration.Flow(
    loginProteinSlot = int("loginProteinSlot") ?: Configuration.Flow().loginProteinSlot,
    globalHotkey = nested("globalHotkey")?.toGlobalHotkey() ?: Configuration.GlobalHotkey(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toGlobalHotkey() = Configuration.GlobalHotkey(
    enabled = boolean("enabled") ?: Configuration.GlobalHotkey().enabled,
    key = string("key") ?: Configuration.GlobalHotkey().key,
    backend = string("backend") ?: Configuration.GlobalHotkey().backend,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toInactivityLimit() = Configuration.InactivityLimit(
    enabled = boolean("enabled") ?: Configuration.InactivityLimit().enabled,
    limitInMinutes = int("limitInMinutes") ?: Configuration.InactivityLimit().limitInMinutes,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toPassword() = Configuration.Password(
    length = int("length") ?: Configuration.Password().length,
    specialCharacters = boolean("specialCharacters") ?: Configuration.Password().specialCharacters,
    promptOnRemoval = boolean("promptOnRemoval") ?: Configuration.Password().promptOnRemoval,
    customPasswordConfigurations = list("customPasswordConfigurations")?.mapIndexed { index, item ->
        item.toNode("$path.customPasswordConfigurations[$index]").toCustomPasswordConfiguration()
    } ?: emptyList(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toTrash() = Configuration.Trash(
    retentionDays = int("retentionDays") ?: Configuration.Trash().retentionDays,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toCustomPasswordConfiguration() = Configuration.CustomPasswordConfiguration(
    name = string("name") ?: Configuration.CustomPasswordConfiguration().name,
    length = int("length") ?: Configuration.CustomPasswordConfiguration().length,
    hasNumbers = boolean("hasNumbers") ?: Configuration.CustomPasswordConfiguration().hasNumbers,
    hasLowercaseLetters = boolean("hasLowercaseLetters") ?: Configuration.CustomPasswordConfiguration().hasLowercaseLetters,
    hasUppercaseLetters = boolean("hasUppercaseLetters") ?: Configuration.CustomPasswordConfiguration().hasUppercaseLetters,
    hasSpecialCharacters = boolean("hasSpecialCharacters") ?: Configuration.CustomPasswordConfiguration().hasSpecialCharacters,
    unusedSpecialCharacters = string("unusedSpecialCharacters") ?: Configuration.CustomPasswordConfiguration().unusedSpecialCharacters,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toYolk() = Configuration.Yolk(
    algorithm = string("algorithm") ?: Configuration.Yolk().algorithm,
    copyToClipboard = boolean("copyToClipboard") ?: Configuration.Yolk().copyToClipboard,
    digits = int("digits") ?: Configuration.Yolk().digits,
    periodSeconds = int("periodSeconds") ?: Configuration.Yolk().periodSeconds,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toAdapter() = Configuration.Adapter(
    clipboard = nested("clipboard")?.toClipboard() ?: Configuration.Clipboard(),
    keyStore = nested("keyStore")?.toKeyStore() ?: Configuration.KeyStore(),
    passwordTree = nested("passwordTree")?.toPasswordTree() ?: Configuration.PasswordTree(),
    userInterface = nested("userInterface")?.toUserInterface() ?: Configuration.UserInterface(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toClipboard() = Configuration.Clipboard(
    nativeTooling = nested("nativeTooling")?.toClipboardNativeTooling() ?: Configuration.ClipboardNativeTooling(),
    reset = nested("reset")?.toClipboardReset() ?: Configuration.ClipboardReset(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toClipboardNativeTooling() = Configuration.ClipboardNativeTooling(
    enabled = boolean("enabled") ?: Configuration.ClipboardNativeTooling().enabled,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toClipboardReset() = Configuration.ClipboardReset(
    enabled = boolean("enabled") ?: Configuration.ClipboardReset().enabled,
    delaySeconds = int("delaySeconds") ?: Configuration.ClipboardReset().delaySeconds,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toPasswordTree() = Configuration.PasswordTree(
    location = string("location") ?: Configuration.PasswordTree().location,
    verifySignature = boolean("verifySignature") ?: Configuration.PasswordTree().verifySignature,
    verifyChecksum = boolean("verifyChecksum") ?: Configuration.PasswordTree().verifyChecksum,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toKeyStore() = Configuration.KeyStore(
    location = string("location") ?: Configuration.KeyStore().location,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toUserInterface() = Configuration.UserInterface(
    ansiEscapeCodes = nested("ansiEscapeCodes")?.toAnsiEscapeCodes() ?: Configuration.AnsiEscapeCodes(),
    audibleBell = boolean("audibleBell") ?: Configuration.UserInterface().audibleBell,
    secureInput = boolean("secureInput") ?: Configuration.UserInterface().secureInput,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toAnsiEscapeCodes() = Configuration.AnsiEscapeCodes(
    enabled = boolean("enabled") ?: Configuration.AnsiEscapeCodes().enabled,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toDomain() = Configuration.Domain(
    eggIdMemory = nested("eggIdMemory")?.toEggIdMemory() ?: Configuration.EggIdMemory(),
    protein = nested("protein")?.toProtein() ?: Configuration.Protein(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toEggIdMemory() = Configuration.EggIdMemory(
    enabled = boolean("enabled") ?: Configuration.EggIdMemory().enabled,
    persisted = boolean("persisted") ?: Configuration.EggIdMemory().persisted,
    updateOnFavoriteUse = boolean("updateOnFavoriteUse") ?: Configuration.EggIdMemory().updateOnFavoriteUse,
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toProtein() = Configuration.Protein(
    secureProteinStructureInput = boolean("secureProteinStructureInput")
        ?: Configuration.Protein().secureProteinStructureInput,
    promptForProteinStructureInputToggle = boolean("promptForProteinStructureInputToggle")
        ?: Configuration.Protein().promptForProteinStructureInputToggle,
    templates = list("templates")?.mapIndexed { index, item ->
        item.toNode("$path.templates[$index]").toProteinTemplate()
    } ?: emptyList(),
).also { ensureFullyConsumed() }

private fun ConfigurationNode.toProteinTemplate(): Configuration.ProteinTemplate {
    val name = string("name") ?: Configuration.ProteinTemplate().name
    val template = Configuration.ProteinTemplate(name = name)
    val slots = values.toMap()
    values.clear()
    slots.forEach { (key, value) ->
        template.putSlot(key, value as? String ?: throw IllegalArgumentException("Protein template '$name' contains invalid slot"))
    }
    ensureFullyConsumed()
    return template
}

private fun ReadableConfiguration.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "application" to application.toYamlValue(),
    "adapter" to adapter.toYamlValue(),
    "domain" to domain.toYamlValue(),
)

private fun ReadableConfiguration.Application.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "backup" to backup.toYamlValue(),
    "exchange" to exchange.toYamlValue(),
    "flow" to flow.toYamlValue(),
    "inactivityLimit" to inactivityLimit.toYamlValue(),
    "password" to password.toYamlValue(),
    "trash" to trash.toYamlValue(),
    "yolk" to yolk.toYamlValue(),
)

private fun ReadableConfiguration.Backup.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "location" to location,
    "numberOfBackups" to numberOfBackups,
    "configuration" to configuration.toYamlValue(),
    "passwordTree" to passwordTree.toYamlValue(),
    "keyStore" to keyStore.toYamlValue(),
)

private fun ReadableConfiguration.BackupSettings.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "enabled" to enabled,
    "location" to location,
    "numberOfBackups" to numberOfBackups,
)

private fun ReadableConfiguration.Exchange.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "promptOnExportFile" to promptOnExportFile,
)

private fun ReadableConfiguration.Flow.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "loginProteinSlot" to loginProteinSlot,
    "globalHotkey" to globalHotkey.toYamlValue(),
)

private fun ReadableConfiguration.GlobalHotkey.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "enabled" to enabled,
    "key" to key,
    "backend" to backend,
)

private fun ReadableConfiguration.InactivityLimit.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "enabled" to enabled,
    "limitInMinutes" to limitInMinutes,
)

private fun ReadableConfiguration.Password.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "length" to length,
    "specialCharacters" to specialCharacters,
    "promptOnRemoval" to promptOnRemoval,
    "customPasswordConfigurations" to customPasswordConfigurations.map { it.toYamlValue() },
)

private fun ReadableConfiguration.Trash.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "retentionDays" to retentionDays,
)

private fun ReadableConfiguration.CustomPasswordConfiguration.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "name" to name,
    "length" to length,
    "hasNumbers" to hasNumbers,
    "hasLowercaseLetters" to hasLowercaseLetters,
    "hasUppercaseLetters" to hasUppercaseLetters,
    "hasSpecialCharacters" to hasSpecialCharacters,
    "unusedSpecialCharacters" to unusedSpecialCharacters,
)

private fun ReadableConfiguration.Yolk.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "algorithm" to algorithm,
    "copyToClipboard" to copyToClipboard,
    "digits" to digits,
    "periodSeconds" to periodSeconds,
)

private fun ReadableConfiguration.Adapter.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "clipboard" to clipboard.toYamlValue(),
    "keyStore" to keyStore.toYamlValue(),
    "passwordTree" to passwordTree.toYamlValue(),
    "userInterface" to userInterface.toYamlValue(),
)

private fun ReadableConfiguration.Clipboard.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "nativeTooling" to nativeTooling.toYamlValue(),
    "reset" to reset.toYamlValue(),
)

private fun ReadableConfiguration.ClipboardNativeTooling.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "enabled" to enabled,
)

private fun ReadableConfiguration.ClipboardReset.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "enabled" to enabled,
    "delaySeconds" to delaySeconds,
)

private fun ReadableConfiguration.PasswordTree.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "location" to location,
    "verifySignature" to verifySignature,
    "verifyChecksum" to verifyChecksum,
)

private fun ReadableConfiguration.KeyStore.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "location" to location,
)

private fun ReadableConfiguration.UserInterface.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "ansiEscapeCodes" to ansiEscapeCodes.toYamlValue(),
    "audibleBell" to audibleBell,
    "secureInput" to secureInput,
)

private fun ReadableConfiguration.AnsiEscapeCodes.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "enabled" to enabled,
)

private fun ReadableConfiguration.Domain.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "eggIdMemory" to eggIdMemory.toYamlValue(),
    "protein" to protein.toYamlValue(),
)

private fun ReadableConfiguration.EggIdMemory.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "enabled" to enabled,
    "persisted" to persisted,
    "updateOnFavoriteUse" to updateOnFavoriteUse,
)

private fun ReadableConfiguration.Protein.toYamlValue(): Map<String, Any?> = linkedMapOf(
    "secureProteinStructureInput" to secureProteinStructureInput,
    "promptForProteinStructureInputToggle" to promptForProteinStructureInputToggle,
    "templates" to templates.map { it.toYamlValue() },
)

private fun ReadableConfiguration.ProteinTemplate.toYamlValue(): Map<String, Any?> = linkedMapOf<String, Any?>("name" to name).apply {
    slots.forEach { (slot, value) -> put(slot.toString(), value) }
}

private class ConfigurationNode(
    val values: MutableMap<String, Any?>,
    val path: String,
) {
    fun nested(name: String): ConfigurationNode? = values.remove(name)?.let { value ->
        value.toNode("$path.$name")
    }

    fun string(name: String): String? = values.remove(name)?.let { value ->
        value as? String ?: throw IllegalArgumentException("$path.$name must be a string")
    }

    fun nullableString(name: String): String? = when (val value = values.remove(name)) {
        null -> null
        is String -> value
        else -> throw IllegalArgumentException("$path.$name must be a string")
    }

    fun boolean(name: String): Boolean? = values.remove(name)?.let { value ->
        value as? Boolean ?: throw IllegalArgumentException("$path.$name must be a boolean")
    }

    fun int(name: String): Int? = values.remove(name)?.let { value ->
        value.toInt("$path.$name")
    }

    fun nullableInt(name: String): Int? = when (val value = values.remove(name)) {
        null -> null
        else -> value.toInt("$path.$name")
    }

    fun list(name: String): List<Any>? = values.remove(name)?.let { value ->
        value as? List<*> ?: throw IllegalArgumentException("$path.$name must be a list")
    }?.mapIndexed { index, item ->
        item ?: throw IllegalArgumentException("$path.$name[$index] must not be null")
    }

    fun ensureFullyConsumed() {
        values.keys.firstOrNull()?.let { key ->
            throw UnrecognizedConfigurationPropertyException("$path.$key".removePrefix("configuration."))
        }
    }
}

private fun Any.toInt(path: String): Int = when (this) {
    is Byte -> toInt()
    is Short -> toInt()
    is Int -> this
    is Long -> toInt().takeIf { it.toLong() == this } ?: throw IllegalArgumentException("$path must be an integer")
    else -> throw IllegalArgumentException("$path must be an integer")
}
