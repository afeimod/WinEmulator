package org.github.ewt45.winemulator.inputcontrols

import android.widget.Button
import org.github.ewt45.winemulator.inputcontrols.TermuxExtraKeysView

/** The {@link Class} that maintains a state of a {@link TermuxX11SpecialButton} */
class TermuxX11SpecialButtonState(termuxExtraKeysView: TermuxExtraKeysView) {

    /** If special button has been created for the {@link TermuxExtraKeysView}. */
    var isCreated = false
    
    /** If special button is active. */
    var isActive = false
        set(value) {
            field = value
            for (button in buttons) {
                button.setTextColor(if (value) mTermuxExtraKeysView.buttonActiveTextColor else mTermuxExtraKeysView.buttonTextColor)
            }
        }
    
    /** If special button is locked due to long hold on it and should not be deactivated if its
     * state is read. */
    var isLocked = false

    val buttons = ArrayList<Button>()

    var mTermuxExtraKeysView: TermuxExtraKeysView = termuxExtraKeysView
        private set
}
