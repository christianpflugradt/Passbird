package de.pflugradts.passbird.application

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.ShellPair

interface ExchangeAdapterPort {
    fun send(data: PasswordInfoMap): TryResult<Unit>
    fun receive(): TryResult<PasswordInfoMap>
}

typealias PasswordInfo = Pair<ShellPair, List<ShellPair>>
typealias PasswordInfoMap = Map<Nest, List<PasswordInfo>>
