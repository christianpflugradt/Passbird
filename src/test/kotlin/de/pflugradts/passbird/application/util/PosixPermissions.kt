package de.pflugradts.passbird.application.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission

fun posixPermissionsIfSupported(path: Path): Set<PosixFilePermission>? =
    Files.getFileAttributeView(path, PosixFileAttributeView::class.java)?.readAttributes()?.permissions()
