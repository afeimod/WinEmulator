package org.github.ewt45.winemulator.inputcontrols

/**
 * Interface for handling input events from virtual controls
 */
interface InputEventHandler {
    /**
     * Handle a key event
     * @param keycode The Android keycode
     * @param isDown True if key is pressed, false if released
     */
    fun onKeyEvent(keycode: Int, isDown: Boolean)

    /**
     * Handle pointer movement
     * @param dx Change in X coordinate
     * @param dy Change in Y coordinate
     */
    fun onPointerMove(dx: Int, dy: Int)

    /**
     * Handle pointer button event
     * @param button Button index (0=left, 1=right, 2=middle)
     * @param isDown True if pressed, false if released
     */
    fun onPointerButton(button: Int, isDown: Boolean)
}
