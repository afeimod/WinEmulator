package org.github.ewt45.winemulator.inputcontrols

/**
 * Represents special buttons in the extra keys view (Ctrl, Alt, Shift, etc.)
 */
enum class TermuxX11SpecialButton(val key: String, val display: String) {
    CTRL("ctrl", "CTRL"),
    ALT("alt", "ALT"),
    SHIFT("shift", "SHIFT"),
    META("meta", "META"),
    FN("fn", "FN");

    companion object {
        fun fromKey(key: String): TermuxX11SpecialButton? {
            return entries.find { it.key == key }
        }
    }
}
