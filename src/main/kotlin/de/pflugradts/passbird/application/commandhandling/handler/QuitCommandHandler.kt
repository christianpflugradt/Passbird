package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

class QuitCommandHandler @Inject constructor(
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject private val systemOperation: SystemOperation,
) : CommandHandler {
    @Subscribe
    private fun handleQuitCommand(@Suppress("UNUSED_PARAMETER") quitCommand: QuitCommand) {
        userInterfaceAdapterPort.send(outputOf(shellOf(randomGoodBye())))
        systemOperation.exit()
    }

    @Suppress("ktlint:standard:max-line-length")
    private fun randomGoodBye() = listOf(
        "Fly safe, my feathered friends, and guard those passwords with the strength of a soaring eagle.",
        "May the winds of security carry you far, and may your nests remain impervious to prying eyes.",
        "Tweet farewell to the passwords, my little hatchlings, and may they stay encrypted in the skies of cyberspace.",
        "Feathered guardians, it's time to spread your wings and protect the digital secrets entrusted to your nests. Farewell!",
        "Fly high, passwords snug in your eggshells, and may your cryptographic codes remain uncrackable in the vast password skies.",
        "Flutter away, my password-protected eggs, and may your encryption be as unyielding as the oak branches that cradle you.",
        "Take flight, my avian keepers of security, and may the passwords you guard be as elusive as the clouds you soar through.",
        "Soar into the sunset, my egg-bound sentinels, and let the passwords within your shells remain as mysterious as the night sky.",
        "Wing your way into the horizon, passwords nestled in your egg-shaped sanctuaries, and may your security be as steadfast as a vigilant owl.",
        "As you take to the skies, remember to keep those passwords safe and sound, for the digital realm relies on your watchful wings.",
        "Fly on, password protectors, and may the clouds of encryption surround you in a cloak of digital secrecy.",
        "Wishing you tailwinds and uncrackable codes as you carry the passwords to their secure destinations. Farewell, my avian custodians.",
        "Spread your wings, guardians of access, and let the passwords you shelter remain as elusive as the dance of sunlight on rippling waters.",
        "May your nests be resilient, and your passwords resilient still. Farewell, feathered keepers of the keys to the digital realm.",
        "As you take flight, may the passwords in your care be as well-protected as the jewels in the crown of the sky.",
        "Sail into the azure expanse, my egg-borne custodians, and let the passwords within your shells be as secure as a fortress in the clouds.",
        "Fly gracefully, my aerial stewards of security, and may the passwords under your vigilant wings remain impervious to the storms of cyber threats.",
        "Wishing you clear skies and encrypted horizons, my password-keeping companions. Until we meet again, soar high and keep the digital keys safe.",
        "May the currents of the digital winds guide you to safeguard the passwords, and may your nests be a haven of unbreakable defenses. Farewell!",
        "Take flight, my encrypted messengers, and may the passwords within your eggs be as well-protected as the ancient secrets whispered by the wind.",
    ).random()
}
