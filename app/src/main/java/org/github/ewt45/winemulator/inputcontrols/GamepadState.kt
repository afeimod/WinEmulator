package org.github.ewt45.winemulator.inputcontrols

/**
 * Gamepad state data holder
 * Added to support gamepad input in InputControlsView
 */
class GamepadState {
    var thumbLX: Float = 0f
    var thumbLY: Float = 0f
    var thumbRX: Float = 0f
    var thumbRY: Float = 0f
    val dpad = booleanArrayOf(false, false, false, false)
    var buttons: Short = 0

    fun setPressed(buttonIdx: Int, pressed: Boolean) {
        val flag = 1 shl buttonIdx
        if (pressed) {
            buttons = (buttons.toInt() or flag).toShort()
        } else {
            buttons = (buttons.toInt() and flag.inv()).toShort()
        }
    }

    fun isPressed(buttonIdx: Int): Boolean {
        return (buttons.toInt() and (1 shl buttonIdx)) != 0
    }

    fun getDPadX(): Byte {
        return if (dpad[1]) 1 else if (dpad[3]) (-1).toByte() else 0
    }

    fun getDPadY(): Byte {
        return if (dpad[0]) (-1).toByte() else if (dpad[2]) 1 else 0
    }
}