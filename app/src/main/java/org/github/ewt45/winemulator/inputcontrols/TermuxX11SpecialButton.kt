package org.github.ewt45.winemulator.inputcontrols

import android.widget.Button

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

/**
 * State holder for a special button.
 * Tracks whether the button is active, locked, and which views are associated with it.
 */
class TermuxX11SpecialButtonState(
    private val view: TermuxExtraKeysView
) {
    var isActive = false
    var isLocked = false
    var isCreated = false
    var buttons = ArrayList<Button>()

    fun setActive(active: Boolean) {
        isActive = active
        if (!active) {
            isLocked = false
        }
        // Update all associated buttons
        for (button in buttons) {
            button.setTextColor(
                if (active) view.buttonActiveTextColor else view.buttonTextColor
            )
        }
    }
}
