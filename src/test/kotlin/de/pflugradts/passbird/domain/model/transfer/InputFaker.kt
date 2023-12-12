package de.pflugradts.passbird.domain.model.transfer

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf

fun fakeInput(withMessage: String = "foo") = inputOf(shellOf(withMessage))

fun fakeInput(withMessage: Char = 'f') = fakeInput("$withMessage")
