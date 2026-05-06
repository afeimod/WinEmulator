package com.termux.x11.controller.xserver

/**
 * Pointer button enumeration for termux-x11 compatibility.
 * Maps to X11 pointer button codes.
 */
class Pointer(private val xServer: Any?) {
    enum class Button {
        BUTTON_LEFT,
        BUTTON_MIDDLE,
        BUTTON_RIGHT,
        BUTTON_SCROLL_UP,
        BUTTON_SCROLL_DOWN,
        BUTTON_SCROLL_CLICK_LEFT,
        BUTTON_SCROLL_CLICK_RIGHT;

        fun code(): Byte = ordinal.plus(1).toByte()

        fun flag(): Int = 1 shl (code().toInt() + MAX_BUTTONS)
    }

    companion object {
        const val MAX_BUTTONS: Byte = 8
    }
}

