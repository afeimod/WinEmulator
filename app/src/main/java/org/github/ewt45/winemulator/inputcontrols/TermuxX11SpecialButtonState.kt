package org.github.ewt45.winemulator.inputcontrols

import android.widget.Button

/**
 * State holder for a special button.
 * Tracks whether the button is active, locked, and which views are associated with it.
 */
class TermuxX11SpecialButtonState(
    private val mTermuxExtraKeysView: TermuxExtraKeysView
) {
    var isActiveVal = false
    var isLocked = false
    var isCreated = false
    var buttons = ArrayList<Button>()

    var isActive: Boolean
        get() = isActiveVal
        set(value) {
            isActiveVal = value
            if (!value) {
                isLocked = false
            }
            // Update all associated buttons
            for (button in buttons) {
                button.setTextColor(
                    if (value) mTermuxExtraKeysView.buttonActiveTextColor else mTermuxExtraKeysView.buttonTextColor
                )
            }
        }

    fun setActive(active: Boolean) {
        isActiveVal = active
        if (!active) {
            isLocked = false
        }
        // Update all associated buttons
        for (button in buttons) {
            button.setTextColor(
                if (active) mTermuxExtraKeysView.buttonActiveTextColor else mTermuxExtraKeysView.buttonTextColor
            )
        }
    }
}
