package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.egg.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.event.ProteinCreated
import de.pflugradts.passbird.domain.model.event.ProteinDiscarded
import de.pflugradts.passbird.domain.model.event.ProteinUpdated
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot

class Egg private constructor(
    private var slot: Slot,
    private val eggId: EggId,
    private val password: Password,
    val proteins: List<MutableOption<Protein>>,
) : AggregateRoot() {

    init {
        registerDomainEvent(EggCreated(this))
    }
    fun associatedNest() = slot
    fun viewEggId() = eggId.view()
    fun viewPassword() = password.view()

    fun rename(eggIdShell: EncryptedShell) {
        eggId.rename(eggIdShell)
        registerDomainEvent(EggRenamed(this))
    }

    fun updatePassword(encryptedShell: EncryptedShell) {
        password.update(encryptedShell)
        registerDomainEvent(EggUpdated(this))
    }
    fun updateProtein(slot: Slot, typeShell: EncryptedShell, structureShell: EncryptedShell) {
        val proteinOption = proteins[slot.index()]
        val previousProtein = proteinOption.orNull()
        val newProtein = createProtein(typeShell, structureShell)
        proteinOption.set(newProtein)
        if (previousProtein != null) {
            registerDomainEvent(ProteinUpdated(this, slot, previousProtein, newProtein))
        } else {
            registerDomainEvent(ProteinCreated(this, newProtein))
        }
    }

    fun moveToNestAt(slot: Slot) {
        this.slot = slot
        registerDomainEvent(EggMoved(this))
    }

    fun discard() {
        password.discard()
        registerDomainEvent(EggDiscarded(this))
    }
    fun discardProtein(slot: Slot) {
        val proteinOption = proteins[slot.index()]
        if (proteinOption.isPresent) {
            val discardedProtein = proteinOption.get()
            proteinOption.set(null)
            registerDomainEvent(ProteinDiscarded(this, discardedProtein))
        }
    }

    override fun equals(other: Any?) = (other as? Egg)?.let {
        it.viewEggId() == viewEggId() && it.associatedNest() == slot
    } ?: false
    override fun hashCode() = slot.hashCode() + 31 * eggId.hashCode()

    companion object {
        fun createEgg(
            slot: Slot,
            eggIdShell: EncryptedShell,
            passwordShell: EncryptedShell,
            proteins: List<MutableOption<Protein>> = emptyProteins(),
        ) = Egg(slot = slot, eggId = createEggId(eggIdShell), password = createPassword(passwordShell), proteins = proteins)
    }
}

private val emptyProteins = {
    listOf<MutableOption<Protein>>(
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
        mutableOptionOf(),
    )
}
