package org.github.ewt45.winemulator.inputcontrols

import androidx.annotation.NonNull

/** The {@link Class} that implements special buttons for {@link TermuxExtraKeysView}. */
class TermuxX11SpecialButton private constructor(@NonNull val key: String) {

    companion object {
        private val map = HashMap<String, TermuxX11SpecialButton>()

        val CTRL = TermuxX11SpecialButton("CTRL")
        val ALT = TermuxX11SpecialButton("ALT")
        val SHIFT = TermuxX11SpecialButton("SHIFT")
        val META = TermuxX11SpecialButton("META")
        val FN = TermuxX11SpecialButton("FN")

        /**
         * Get the {@link TermuxX11SpecialButton} for `key`.
         *
         * @param key The unique key name for the special button.
         */
        fun valueOf(key: String): TermuxX11SpecialButton? {
            return map[key]
        }
    }

    init {
        map[key] = this
    }

    /** Get the key for this {@link TermuxX11SpecialButton}. */
    fun getKey(): String {
        return key
    }

    @NonNull
    override fun toString(): String {
        return key
    }
}
