package de.pflugradts.passbird.application.eventhandling
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.ddd.DomainEvent
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.event.NestCreated
import de.pflugradts.passbird.domain.model.event.NestDiscarded
import de.pflugradts.passbird.domain.model.event.ProteinCreated
import de.pflugradts.passbird.domain.model.event.ProteinDiscarded
import de.pflugradts.passbird.domain.model.event.ProteinUpdated
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.EVENT_HANDLED
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
class ApplicationEventHandler constructor(
    private val cryptoProvider: CryptoProvider,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : EventHandler {
    override val eventTypes: Set<Class<out DomainEvent>> = setOf(
        EggCreated::class.java,
        EggUpdated::class.java,
        EggRenamed::class.java,
        EggDiscarded::class.java,
        EggMoved::class.java,
        EggNotFound::class.java,
        EggsExported::class.java,
        EggsImported::class.java,
        ProteinCreated::class.java,
        ProteinUpdated::class.java,
        ProteinDiscarded::class.java,
        NestCreated::class.java,
        NestDiscarded::class.java,
    )

    override fun handle(domainEvent: DomainEvent) {
        when (domainEvent) {
            is EggCreated -> handleEggCreated(domainEvent)
            is EggUpdated -> handleEggUpdated(domainEvent)
            is EggRenamed -> handleEggRenamed(domainEvent)
            is EggDiscarded -> handleEggDiscarded(domainEvent)
            is EggMoved -> handleEggMoved(domainEvent)
            is EggNotFound -> handleEggNotFound(domainEvent)
            is EggsExported -> handleEggsExported(domainEvent)
            is EggsImported -> handleEggsImported(domainEvent)
            is ProteinCreated -> handleProteinCreated(domainEvent)
            is ProteinUpdated -> handleProteinUpdated(domainEvent)
            is ProteinDiscarded -> handleProteinDiscarded(domainEvent)
            is NestCreated -> handleNestCreated(domainEvent)
            is NestDiscarded -> handleNestDiscarded(domainEvent)
        }
    }

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
        val proteinType = decrypt(proteinCreated.protein.viewType())
        val eggId = decrypt(proteinCreated.egg.viewEggId())
        val msg = "Protein '$proteinType' for egg '$eggId' successfully created."
        send(msg)
    }

    private fun handleProteinUpdated(proteinUpdated: ProteinUpdated) {
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
