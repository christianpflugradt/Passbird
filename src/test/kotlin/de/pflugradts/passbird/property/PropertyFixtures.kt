package de.pflugradts.passbird.property

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
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators

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

fun passwordTreeFixtures(): Arbitrary<PasswordTreeFixture> = explicitNests().flatMap { nests ->
    val slots = listOf(Slot.DEFAULT) + nests.keys.sortedBy(Slot::index)
    Combinators.combine(plainEggs(slots), favoriteEntries(), memoryEntries()).`as` { eggs, favorites, memory ->
        PasswordTreeFixture(nests = nests, eggs = eggs, favorites = favorites, memory = memory)
    }
}

fun exchangeFixtures(): Arbitrary<ExchangeFixture> = exchangeSlots().flatMap { slots ->
    nonEmptyTextValues().list().ofSize(slots.size).flatMap { nestIds ->
        plainPasswordInfoLists().list().ofSize(slots.size).map { eggLists ->
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

fun textValues(): Arbitrary<String> = Arbitraries.strings()
    .withChars(ALLOWED_TEXT_CHARACTERS)
    .ofMaxLength(16)

fun byteContents(): Arbitrary<List<Byte>> = Arbitraries.bytes().list().ofMaxSize(128)

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

private fun explicitNests(): Arbitrary<Map<Slot, String>> = nonDefaultSlots().flatMap { slots ->
    nonEmptyTextValues().list().ofSize(slots.size).map { nestIds ->
        slots.zip(nestIds).toMap().toSortedMap(compareBy(Slot::index))
    }
}

private fun nonDefaultSlots(): Arbitrary<List<Slot>> =
    Arbitraries.of(nonDefaultSlotEntries).list().uniqueElements().ofMaxSize(nonDefaultSlotEntries.size)

private fun exchangeSlots(): Arbitrary<List<Slot>> =
    Arbitraries.of(Slot.entries).list().uniqueElements().ofMinSize(1).ofMaxSize(Slot.entries.size)

private fun plainEggs(slots: List<Slot>): Arbitrary<List<PlainEggData>> = plainEggData(slots).list().ofMaxSize(12)

private fun plainEggData(slots: List<Slot>): Arbitrary<PlainEggData> = Combinators.combine(
    Arbitraries.of(slots),
    textValues(),
    textValues(),
    proteinEntries(),
).`as` { slot, eggId, password, proteins ->
    PlainEggData(slot, eggId, password, proteins)
}

private fun memoryEntries(): Arbitrary<Map<MemoryCell, String>> =
    Arbitraries.of(allMemoryCells).list().uniqueElements().ofMaxSize(12).flatMap { cells ->
        textValues().list().ofSize(cells.size).map { values ->
            cells.zip(values).toMap().toSortedMap(compareBy(MemoryCell::nestSlot, MemoryCell::memorySlot))
        }
    }

private fun favoriteEntries(): Arbitrary<Map<FavoriteCell, String>> =
    Arbitraries.of(allFavoriteCells).list().uniqueElements().ofMaxSize(12).flatMap { cells ->
        textValues().list().ofSize(cells.size).map { values ->
            cells.zip(values).toMap().toSortedMap(compareBy(FavoriteCell::nestSlot, FavoriteCell::favoriteSlot))
        }
    }

private fun plainPasswordInfoLists(): Arbitrary<List<PlainPasswordInfoData>> = plainPasswordInfoData().list().ofMaxSize(6)

private fun plainPasswordInfoData(): Arbitrary<PlainPasswordInfoData> = Combinators.combine(
    textValues(),
    textValues(),
    proteinEntries(),
).`as` { eggId, password, proteins ->
    PlainPasswordInfoData(eggId, password, proteins)
}

private fun proteinEntries(): Arbitrary<Map<Slot, Pair<String, String>>> =
    Arbitraries.of(Slot.entries).list().uniqueElements().ofMaxSize(Slot.entries.size).flatMap { slots ->
        nonEmptyProteinPairs().list().ofSize(slots.size).map { proteins ->
            slots.zip(proteins).toMap().toSortedMap(compareBy(Slot::index))
        }
    }

private fun PasswordTreeFixture.toNestShells() = Slot.entries
    .filterNot { it == Slot.DEFAULT }
    .map { slot -> nests[slot]?.let(::shellOf) ?: emptyShell() }

private fun nonEmptyProteinPairs(): Arbitrary<Pair<String, String>> =
    Combinators.combine(nonEmptyTextValues(), nonEmptyTextValues()).`as`(::Pair)

private fun nonEmptyTextValues(): Arbitrary<String> = Arbitraries.strings()
    .withChars(ALLOWED_TEXT_CHARACTERS)
    .ofMinLength(1)
    .ofMaxLength(16)

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

private fun PlainPasswordInfoData.toPasswordInfo() = (shellOf(eggId) to shellOf(password)) to MutableList(Slot.entries.size) {
    emptyProteinPair
}.apply {
    proteins.forEach { (slot, shells) -> this[slot.index()] = shellOf(shells.first) to shellOf(shells.second) }
}

private val ALLOWED_TEXT_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !@#\$%^&*()_-+=[]{}:;,.?/|~\t\n"
private val nonDefaultSlotEntries = Slot.entries.filterNot { it == Slot.DEFAULT }
private val allFavoriteCells = Slot.entries.flatMap { nestSlot ->
    Slot.entries.map { favoriteSlot -> FavoriteCell(nestSlot, favoriteSlot) }
}
private val allMemoryCells = Slot.entries.flatMap { nestSlot -> Slot.entries.map { memorySlot -> MemoryCell(nestSlot, memorySlot) } }
private val emptyProteinPair = emptyShell() to emptyShell()
