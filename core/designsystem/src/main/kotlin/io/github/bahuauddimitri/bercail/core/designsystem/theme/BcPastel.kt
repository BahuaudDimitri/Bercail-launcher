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
        /**
         * The same key (a conversation, a lamp) always gets the same pastel. Keys in [ranked] (the favorite
         * people) take the pastels in their order, so that two of them never share one.
         */
        fun forKey(key: String, ranked: List<String> = emptyList()): BcPastel {
            val rank = ranked.indexOf(key)
            return entries[if (rank >= 0) rank % entries.size else Math.floorMod(key.hashCode(), entries.size)]
        }
    }
}
