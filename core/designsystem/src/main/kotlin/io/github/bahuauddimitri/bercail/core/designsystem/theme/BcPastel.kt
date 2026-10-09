package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** The five pastels a screen can give to a person or a home control. */
enum class BcPastel(internal val color: Color) {
    Coral(BcColors.pastelCoral),
    Blue(BcColors.pastelBlue),
    Mint(BcColors.pastelMint),
    Lavender(BcColors.pastelLavender),
    Apricot(BcColors.pastelApricot)
    ;

    companion object {
        /** The same key (a conversation, a lamp) always gets the same pastel. */
        fun forKey(key: String): BcPastel = entries[Math.floorMod(key.hashCode(), entries.size)]
    }
}
