package de.pflugradts.passbird.property

import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.application.PasswordInfoMap
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string

data class PlainEggData(
    val nestSlot: Slot,
    val eggId: String,
    val password: String,
    val proteins: Map<Slot, Pair<String, String>>,
)

data class MemoryCell(val nestSlot: Slot, val memorySlot: Slot)
data class FavoriteCell(val nestSlot: Slot, val favoriteSlot: Slot)

data class PasswordTreeFixture(
    val nests: Map<Slot, String>,
    val eggs: List<PlainEggData>,
    val favorites: Map<FavoriteCell, String>,
    val memory: Map<MemoryCell, String>,
)

data class ExchangeNestData(
    val nestId: String,
    val eggs: List<PlainPasswordInfoData>,
)

data class PlainPasswordInfoData(
    val eggId: String,
    val password: String,
    val proteins: Map<Slot, Pair<String, String>>,
)

data class ExchangeFixture(val nests: Map<Slot, ExchangeNestData>)

fun passwordTreeFixtures(): Arb<PasswordTreeFixture> = explicitNests().flatMap { nests ->
    val slots = listOf(Slot.DEFAULT) + nests.keys.sortedBy(Slot::index)
    Arb.bind(plainEggs(slots), favoriteEntries(), memoryEntries()) { eggs, favorites, memory ->
        PasswordTreeFixture(nests = nests, eggs = eggs, favorites = favorites, memory = memory)
    }
}

fun exchangeFixtures(): Arb<ExchangeFixture> = exchangeSlots().flatMap { slots ->
    Arb.list(nonBlankTextValues(), slots.size..slots.size).flatMap { nestIds ->
        Arb.list(plainPasswordInfoLists(), slots.size..slots.size).map { eggLists ->
            ExchangeFixture(
                slots.indices.associate { index ->
                    slots[index] to ExchangeNestData(
                        nestId = nestIds[index],
                        eggs = eggLists[index],
                    )
                },
            )
        }
    }
}

fun textValues(): Arb<String> = Arb.string(0..16, ALLOWED_TEXT_CHARACTERS)

fun byteContents(): Arb<List<Byte>> = Arb.list(Arb.byte(), 0..128)

fun PasswordTreeFixture.toEggStreamSupplier(cryptoProvider: CryptoProvider): EggStreamSupplier {
    val eggs = eggs.map { egg -> egg.toEgg(cryptoProvider) }
    return EggStreamSupplier({ eggs.stream() }, toMemoryMap(cryptoProvider), toFavoriteMap(cryptoProvider), toNestShells())
}

fun PasswordTreeFixture.populateNests(nestService: NestService) {
    nests.forEach { (slot, nestId) -> nestService.place(shellOf(nestId), slot) }
}

fun PasswordTreeFixture.normalizedExplicitNests(): Map<Slot, String> = nests.toSortedMap(compareBy(Slot::index))

fun normalizeExplicitNests(nestService: NestService): Map<Slot, String> = Slot.entries
    .filterNot { it == Slot.DEFAULT }
    .mapNotNull { slot ->
        nestService.atNestSlot(slot).orNull()?.let { slot to it.viewNestId().asString() }
    }.toMap().toSortedMap(compareBy(Slot::index))

fun normalizeExplicitNests(nestShells: List<Shell>): Map<Slot, String> = nestShells.mapIndexedNotNull { index, nestShell ->
    nestShell.takeUnless(Shell::isEmpty)?.let { Slot.slotAt(index + 1) to it.asString() }
}.toMap().toSortedMap(compareBy(Slot::index))

fun PasswordTreeFixture.normalizedEggs(): List<PlainEggData> = eggs
fun PasswordTreeFixture.normalizedFavorites(): Map<FavoriteCell, String> =
    favorites.toSortedMap(compareBy(FavoriteCell::nestSlot, FavoriteCell::favoriteSlot))

fun normalizeEggs(eggs: List<Egg>, cryptoProvider: CryptoProvider): List<PlainEggData> = eggs.map { egg ->
    PlainEggData(
        nestSlot = egg.associatedNest(),
        eggId = cryptoProvider.decrypt(egg.viewEggId()).asString(),
        password = cryptoProvider.decrypt(egg.viewPassword()).asString(),
        proteins = Slot.entries.mapNotNull { slot ->
            egg.proteins[slot.index()].orNull()?.let { protein ->
                slot to
                    (cryptoProvider.decrypt(protein.viewType()).asString() to cryptoProvider.decrypt(protein.viewStructure()).asString())
            }
        }.toMap().toSortedMap(compareBy(Slot::index)),
    )
}

fun PasswordTreeFixture.normalizedMemory(): Map<MemoryCell, String> =
    memory.toSortedMap(compareBy(MemoryCell::nestSlot, MemoryCell::memorySlot))

fun normalizeMemory(memory: MemoryMap, cryptoProvider: CryptoProvider): Map<MemoryCell, String> = buildMap {
    Slot.entries.forEach { nestSlot ->
        memory[nestSlot].get().let { eggIdMemory ->
            Slot.entries.forEach { memorySlot ->
                eggIdMemory[memorySlot].ifPresent { encryptedShell ->
                    put(MemoryCell(nestSlot, memorySlot), cryptoProvider.decrypt(encryptedShell).asString())
                }
            }
        }
    }
}.toSortedMap(compareBy(MemoryCell::nestSlot, MemoryCell::memorySlot))

fun normalizeFavorites(favorites: FavoriteMap, cryptoProvider: CryptoProvider): Map<FavoriteCell, String> = buildMap {
    Slot.entries.forEach { nestSlot ->
        favorites[nestSlot].get().let { eggIdFavorites ->
            Slot.entries.forEach { favoriteSlot ->
                eggIdFavorites[favoriteSlot].ifPresent { encryptedShell ->
                    put(FavoriteCell(nestSlot, favoriteSlot), cryptoProvider.decrypt(encryptedShell).asString())
                }
            }
        }
    }
}.toSortedMap(compareBy(FavoriteCell::nestSlot, FavoriteCell::favoriteSlot))

fun ExchangeFixture.toPasswordInfoMap(): PasswordInfoMap = nests.entries.associate { (slot, nestData) ->
    createNest(shellOf(nestData.nestId), slot) to nestData.eggs.map(PlainPasswordInfoData::toPasswordInfo)
}

fun normalizePasswordInfoMap(passwordInfoMap: PasswordInfoMap): ExchangeFixture = ExchangeFixture(
    passwordInfoMap.entries.associate { (nest, eggs) ->
        nest.slot to ExchangeNestData(
            nestId = nest.viewNestId().asString(),
            eggs = eggs.map { passwordInfo ->
                PlainPasswordInfoData(
                    eggId = passwordInfo.first.first.asString(),
                    password = passwordInfo.first.second.asString(),
                    proteins = passwordInfo.second.mapIndexedNotNull { index, shellPair ->
                        shellPair.takeUnless { it == emptyProteinPair }?.let {
                            Slot.slotAt(index) to
                                (it.first.asString() to it.second.asString())
                        }
                    }.toMap().toSortedMap(compareBy(Slot::index)),
                )
            },
        )
    }.toSortedMap(compareBy(Slot::index)),
)

private fun explicitNests(): Arb<Map<Slot, String>> = nonDefaultSlots().flatMap { slots ->
    Arb.list(nonBlankTextValues(), slots.size..slots.size).map { nestIds ->
        slots.zip(nestIds).toMap().toSortedMap(compareBy(Slot::index))
    }
}

private fun nonDefaultSlots(): Arb<List<Slot>> = Arb.set(Arb.element(nonDefaultSlotEntries), 0..nonDefaultSlotEntries.size).map { slots ->
    slots.sortedBy(Slot::index)
}

private fun exchangeSlots(): Arb<List<Slot>> =
    Arb.set(Arb.element(Slot.entries), 1..Slot.entries.size).map { slots -> slots.sortedBy(Slot::index) }

private fun plainEggs(slots: List<Slot>): Arb<List<PlainEggData>> = Arb.list(plainEggData(slots), 0..12)

private fun plainEggData(slots: List<Slot>): Arb<PlainEggData> = Arb.bind(
    Arb.element(slots),
    textValues(),
    textValues(),
    proteinEntries(),
) { slot, eggId, password, proteins ->
    PlainEggData(slot, eggId, password, proteins)
}

private fun memoryEntries(): Arb<Map<MemoryCell, String>> = Arb.set(Arb.element(allMemoryCells), 0..12).flatMap { generatedCells ->
    val cells = generatedCells.sortedWith(compareBy(MemoryCell::nestSlot, MemoryCell::memorySlot))
    Arb.list(textValues(), cells.size..cells.size).map { values ->
        cells.zip(values).toMap().toSortedMap(compareBy(MemoryCell::nestSlot, MemoryCell::memorySlot))
    }
}

private fun favoriteEntries(): Arb<Map<FavoriteCell, String>> = Arb.set(Arb.element(allFavoriteCells), 0..12).flatMap { generatedCells ->
    val cells = generatedCells.sortedWith(compareBy(FavoriteCell::nestSlot, FavoriteCell::favoriteSlot))
    Arb.list(textValues(), cells.size..cells.size).map { values ->
        cells.zip(values).toMap().toSortedMap(compareBy(FavoriteCell::nestSlot, FavoriteCell::favoriteSlot))
    }
}

private fun plainPasswordInfoLists(): Arb<List<PlainPasswordInfoData>> = Arb.list(plainPasswordInfoData(), 0..6).map {
    it.distinctBy(PlainPasswordInfoData::eggId)
}

private fun plainPasswordInfoData(): Arb<PlainPasswordInfoData> = Arb.bind(
    eggIdValues(),
    textValues(),
    proteinEntries(),
) { eggId, password, proteins ->
    PlainPasswordInfoData(eggId, password, proteins)
}

private fun proteinEntries(): Arb<Map<Slot, Pair<String, String>>> =
    Arb.set(Arb.element(Slot.entries), 0..Slot.entries.size).flatMap { generatedSlots ->
        val slots = generatedSlots.sortedBy(Slot::index)
        Arb.list(nonEmptyProteinPairs(), slots.size..slots.size).map { proteins ->
            slots.zip(proteins).toMap().toSortedMap(compareBy(Slot::index))
        }
    }

private fun PasswordTreeFixture.toNestShells() = Slot.entries
    .filterNot { it == Slot.DEFAULT }
    .map { slot -> nests[slot]?.let(::shellOf) ?: emptyShell() }

private fun nonEmptyProteinPairs(): Arb<Pair<String, String>> = Arb.bind(nonEmptyTextValues(), nonEmptyTextValues(), ::Pair)

private fun nonBlankTextValues(): Arb<String> = Arb.bind(
    Arb.string(0..15, ALLOWED_TEXT_CHARACTERS),
    Arb.element(NON_BLANK_TEXT_CHARACTERS.toList()),
) { prefix, marker -> prefix + marker }

private fun nonEmptyTextValues(): Arb<String> = Arb.string(1..16, ALLOWED_TEXT_CHARACTERS)

private fun eggIdValues(): Arb<String> = Arb.bind(
    Arb.element(EGG_ID_FIRST_CHARACTERS.toList()),
    Arb.string(0..15, EGG_ID_REMAINING_CHARACTERS),
) { first, remaining -> "$first$remaining" }

private fun PlainEggData.toEgg(cryptoProvider: CryptoProvider): Egg = createEgg(
    slot = nestSlot,
    eggIdShell = cryptoProvider.encrypt(shellOf(eggId)),
    passwordShell = cryptoProvider.encrypt(shellOf(password)),
).apply {
    this@toEgg.proteins.forEach { (slot, protein) ->
        updateProtein(slot, cryptoProvider.encrypt(shellOf(protein.first)), cryptoProvider.encrypt(shellOf(protein.second)))
    }
}

private fun PasswordTreeFixture.toMemoryMap(cryptoProvider: CryptoProvider): MemoryMap = Slots<EggIdMemory>().apply {
    Slot.entries.forEach { nestSlot ->
        this[nestSlot] = toEggIdMemory(nestSlot, cryptoProvider)
    }
}

private fun PasswordTreeFixture.toFavoriteMap(cryptoProvider: CryptoProvider): FavoriteMap = Slots<EggIdFavorites>().apply {
    Slot.entries.forEach { nestSlot ->
        this[nestSlot] = toEggIdFavorites(nestSlot, cryptoProvider)
    }
}

private fun PasswordTreeFixture.toEggIdMemory(nestSlot: Slot, cryptoProvider: CryptoProvider): EggIdMemory = EggIdMemory().apply {
    Slot.entries.forEach { memorySlot ->
        memory[MemoryCell(nestSlot, memorySlot)]?.let { eggId ->
            this[memorySlot].set(cryptoProvider.encrypt(shellOf(eggId)))
        }
    }
}

private fun PasswordTreeFixture.toEggIdFavorites(nestSlot: Slot, cryptoProvider: CryptoProvider): EggIdFavorites = EggIdFavorites().apply {
    Slot.entries.forEach { favoriteSlot ->
        favorites[FavoriteCell(nestSlot, favoriteSlot)]?.let { eggId ->
            assign(favoriteSlot, cryptoProvider.encrypt(shellOf(eggId)))
        }
    }
}

private fun PlainPasswordInfoData.toPasswordInfo() = PasswordInfo(
    first = shellOf(eggId) to shellOf(password),
    second = MutableList(Slot.entries.size) {
        emptyProteinPair
    }.apply {
        proteins.forEach { (slot, shells) -> this[slot.index()] = shellOf(shells.first) to shellOf(shells.second) }
    },
)

private val ALLOWED_TEXT_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !@#\$%^&*()_-+=[]{}:;,.?/|~\t\n"
private val NON_BLANK_TEXT_CHARACTERS = ALLOWED_TEXT_CHARACTERS.filterNot(Char::isWhitespace)
private val EGG_ID_FIRST_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
private val EGG_ID_REMAINING_CHARACTERS = EGG_ID_FIRST_CHARACTERS + "0123456789"
private val nonDefaultSlotEntries = Slot.entries.filterNot { it == Slot.DEFAULT }
private val allFavoriteCells = Slot.entries.flatMap { nestSlot ->
    Slot.entries.map { favoriteSlot -> FavoriteCell(nestSlot, favoriteSlot) }
}
private val allMemoryCells = Slot.entries.flatMap { nestSlot -> Slot.entries.map { memorySlot -> MemoryCell(nestSlot, memorySlot) } }
private val emptyProteinPair = emptyShell() to emptyShell()
