package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.kotlinextensions.MutableOption.Companion.emptyOption
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.egg.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot

class Egg private constructor(
    private var slot: Slot,
    private val eggId: EggId,
    private val password: Password,
    val proteins: List<Option<Protein>>,
) : AggregateRoot() {

    init {
        registerDomainEvent(EggCreated(this))
    }
    fun associatedNest() = slot
    fun viewEggId() = eggId.view()
    fun viewPassword() = password.view()

    fun rename(eggIdShell: Shell) {
        eggId.rename(eggIdShell)
        registerDomainEvent(EggRenamed(this))
    }

    fun updatePassword(shell: Shell) {
        password.update(shell)
        registerDomainEvent(EggUpdated(this))
    }

    fun moveToNestAt(slot: Slot) {
        this.slot = slot
        registerDomainEvent(EggMoved(this))
    }

    fun discard() {
        password.discard()
        registerDomainEvent(EggDiscarded(this))
    }

    override fun equals(other: Any?) = (other as? Egg)?.let {
        it.viewEggId() == viewEggId() && it.associatedNest() == slot
    } ?: false
    override fun hashCode() = slot.hashCode() + 31 * eggId.hashCode()

    companion object {
        fun createEgg(slot: Slot, eggIdShell: Shell, passwordShell: Shell, proteins: List<Option<Protein>> = emptyProteins()) =
            Egg(slot, createEggId(eggIdShell), createPassword(passwordShell), proteins)
    }
}

private val emptyProteins = {
    listOf<Option<Protein>>(
        emptyOption(),
        emptyOption(),
        emptyOption(),
        emptyOption(),
        emptyOption(),
        emptyOption(),
        emptyOption(),
        emptyOption(),
        emptyOption(),
        emptyOption(),
    )
}
