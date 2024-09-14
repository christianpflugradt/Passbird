package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.ShellPair

interface ExchangeAdapterPort {
    fun send(data: PasswordInfoMap)
    fun receive(): PasswordInfoMap
}

typealias PasswordInfo = Pair<ShellPair, List<ShellPair>>
typealias PasswordInfoMap = Map<Nest, List<PasswordInfo>>
