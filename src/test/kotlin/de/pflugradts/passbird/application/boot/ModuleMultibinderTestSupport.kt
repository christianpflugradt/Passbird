package de.pflugradts.passbird.application.boot

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import java.lang.reflect.Modifier

private const val ROOT = "de.pflugradts.passbird"
private val productionClasses = ClassFileImporter().withImportOption(DoNotIncludeTests()).importPackages(ROOT)

internal fun expectedMultibinderClasses(type: Class<*>, vararg excludedTypes: Class<*>) = productionClasses
    .map { Class.forName(it.name) }
    .filter { it.isConcreteImplementationOf(type) }
    .filterNot { candidate -> excludedTypes.any { it.isAssignableFrom(candidate) } }
    .toSet()

internal fun <T : Any> Iterable<T>.implementationClasses(): Set<Class<*>> = map { it::class.java }.toSet()

private fun Class<*>.isConcreteImplementationOf(type: Class<*>) =
    type.isAssignableFrom(this) && !isInterface && !Modifier.isAbstract(modifiers)
