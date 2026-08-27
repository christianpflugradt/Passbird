package de.pflugradts.passbird.application.eventhandling
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggRestored
import de.pflugradts.passbird.domain.model.event.EggTrashed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.event.NestCreated
import de.pflugradts.passbird.domain.model.event.NestDiscarded
import de.pflugradts.passbird.domain.model.event.ProteinCreated
import de.pflugradts.passbird.domain.model.event.ProteinDiscarded
import de.pflugradts.passbird.domain.model.event.ProteinUpdated
import de.pflugradts.passbird.domain.model.event.YolkDiscarded
import de.pflugradts.passbird.domain.model.event.YolkUpdated
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.EVENT_HANDLED
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
class ApplicationEventHandler constructor(
    private val cryptoProvider: CryptoProvider,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val proteinEventOutputControl: ProteinEventOutputControl = ProteinEventOutputControl(),
) : EventHandler {
    override val eventTypes: Set<Class<out DomainEvent>> = setOf(
        EggCreated::class.java,
        EggUpdated::class.java,
        EggRenamed::class.java,
        EggDiscarded::class.java,
        EggRestored::class.java,
        EggTrashed::class.java,
        EggMoved::class.java,
        EggNotFound::class.java,
        EggsExported::class.java,
        EggsImported::class.java,
        ProteinCreated::class.java,
        ProteinUpdated::class.java,
        ProteinDiscarded::class.java,
        YolkUpdated::class.java,
        YolkDiscarded::class.java,
        NestCreated::class.java,
        NestDiscarded::class.java,
    )
    private val handlers: Map<Class<out DomainEvent>, (DomainEvent) -> Unit> = mapOf(
        EggCreated::class.java to { handleEggCreated(it as EggCreated) },
        EggUpdated::class.java to { handleEggUpdated(it as EggUpdated) },
        EggRenamed::class.java to { handleEggRenamed(it as EggRenamed) },
        EggDiscarded::class.java to { handleEggDiscarded(it as EggDiscarded) },
        EggRestored::class.java to { handleEggRestored(it as EggRestored) },
        EggTrashed::class.java to { handleEggTrashed(it as EggTrashed) },
        EggMoved::class.java to { handleEggMoved(it as EggMoved) },
        EggNotFound::class.java to { handleEggNotFound(it as EggNotFound) },
        EggsExported::class.java to { handleEggsExported(it as EggsExported) },
        EggsImported::class.java to { handleEggsImported(it as EggsImported) },
        ProteinCreated::class.java to { handleProteinCreated(it as ProteinCreated) },
        ProteinUpdated::class.java to { handleProteinUpdated(it as ProteinUpdated) },
        ProteinDiscarded::class.java to { handleProteinDiscarded(it as ProteinDiscarded) },
        YolkUpdated::class.java to { handleYolkUpdated(it as YolkUpdated) },
        YolkDiscarded::class.java to { handleYolkDiscarded(it as YolkDiscarded) },
        NestCreated::class.java to { handleNestCreated(it as NestCreated) },
        NestDiscarded::class.java to { handleNestDiscarded(it as NestDiscarded) },
    )

    override fun handle(domainEvent: DomainEvent) = handlers.getValue(domainEvent.javaClass)(domainEvent)

    private fun handleEggCreated(eggCreated: EggCreated) {
        send("Egg '${decrypt(eggCreated.egg.viewEggId())}' successfully created.")
    }

    private fun handleEggUpdated(eggUpdated: EggUpdated) {
        send("Egg '${decrypt(eggUpdated.egg.viewEggId())}' successfully updated.")
    }

    private fun handleEggRenamed(eggRenamed: EggRenamed) {
        send("Egg '${decrypt(eggRenamed.egg.viewEggId())}' successfully renamed.")
    }

    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        send("Egg '${decrypt(eggDiscarded.egg.viewEggId())}' successfully discarded.")
    }

    private fun handleEggRestored(eggRestored: EggRestored) {
        send("Egg '${decrypt(eggRestored.egg.viewEggId())}' successfully restored.")
    }

    private fun handleEggTrashed(eggTrashed: EggTrashed) {
        send("Egg '${decrypt(eggTrashed.egg.viewEggId())}' successfully moved to trash.")
    }

    private fun handleEggMoved(eggMoved: EggMoved) {
        send("Egg '${decrypt(eggMoved.egg.viewEggId())}' successfully moved to ${nestSlotText(eggMoved.egg.associatedNest().index())}.")
    }

    private fun handleEggNotFound(eggNotFound: EggNotFound) {
        send("Egg '${eggNotFound.eggIdShell.asString()}' not found.")
        userInterfaceAdapterPort.warningSound()
    }

    private fun handleEggsExported(eggsExported: EggsExported) {
        send("${eggsExported.count} eggs successfully exported.")
    }

    private fun handleEggsImported(eggsImported: EggsImported) {
        send("${eggsImported.count} eggs successfully imported.")
    }

    private fun handleProteinCreated(proteinCreated: ProteinCreated) {
        if (proteinEventOutputControl.proteinEventsSuppressed()) return
        val proteinType = decrypt(proteinCreated.protein.viewType())
        val eggId = decrypt(proteinCreated.egg.viewEggId())
        val msg = "Protein '$proteinType' for egg '$eggId' successfully created."
        send(msg)
    }

    private fun handleProteinUpdated(proteinUpdated: ProteinUpdated) {
        if (proteinEventOutputControl.proteinEventsSuppressed()) return
        val oldProteinType = decrypt(proteinUpdated.oldProtein.viewType())
        val newProteinType = decrypt(proteinUpdated.newProtein.viewType())
        val eggId = decrypt(proteinUpdated.egg.viewEggId())
        val msg = if (oldProteinType == newProteinType) {
            "Protein '$oldProteinType' at slot ${proteinUpdated.slot.index()} for egg '$eggId' successfully updated."
        } else {
            "Protein for egg '$eggId' at slot ${proteinUpdated.slot.index()} successfully updated " +
                "from '$oldProteinType' to '$newProteinType'."
        }
        send(msg)
    }

    private fun handleProteinDiscarded(proteinDiscarded: ProteinDiscarded) {
        val proteinType = decrypt(proteinDiscarded.protein.viewType())
        val eggId = decrypt(proteinDiscarded.egg.viewEggId())
        val msg = "Protein '$proteinType' of egg '$eggId' successfully discarded."
        send(msg)
    }

    private fun handleYolkUpdated(yolkUpdated: YolkUpdated) {
        send("Yolk of egg '${decrypt(yolkUpdated.egg.viewEggId())}' successfully updated.")
    }

    private fun handleYolkDiscarded(yolkDiscarded: YolkDiscarded) {
        send("Yolk of egg '${decrypt(yolkDiscarded.egg.viewEggId())}' successfully discarded.")
    }

    private fun handleNestCreated(nestCreated: NestCreated) {
        send("Nest '${nestCreated.nest.viewNestId().asString()}' successfully created.")
    }

    private fun handleNestDiscarded(nestDiscarded: NestDiscarded) {
        send("Nest '${nestDiscarded.nest.viewNestId().asString()}' successfully discarded.")
    }
    private fun send(str: String) = userInterfaceAdapterPort.send(outputOf(shellOf(str), EVENT_HANDLED))
    private fun decrypt(encryptedShell: EncryptedShell): String {
        val decryptedShell = cryptoProvider.decrypt(encryptedShell)
        return try {
            decryptedShell.asString()
        } finally {
            decryptedShell.scramble()
        }
    }
}
private fun nestSlotText(nestSlotIndex: Int) = if (nestSlotIndex in 1..9) "Nest at Slot $nestSlotIndex" else "Default Nest"
