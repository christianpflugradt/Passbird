package de.pflugradts.passbird.application

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair

interface ExchangeAdapterPort {
    fun send(data: PasswordInfoMap): TryResult<Unit>
    fun receive(): TryResult<PasswordInfoMap>
}

data class PasswordYolkInfo(
    val secret: Shell,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
)

data class PasswordInfo(
    val first: ShellPair,
    val second: List<ShellPair>,
    val yolk: PasswordYolkInfo? = null,
)

typealias PasswordInfoMap = Map<Nest, List<PasswordInfo>>
