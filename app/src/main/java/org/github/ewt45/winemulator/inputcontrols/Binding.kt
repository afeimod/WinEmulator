package org.github.ewt45.winemulator.inputcontrols

import androidx.annotation.NonNull
import com.termux.x11.controller.xserver.Pointer
import com.termux.x11.controller.xserver.XKeycode

enum class Binding {
    NONE,
    MOUSE_LEFT_BUTTON,
    MOUSE_MIDDLE_BUTTON,
    MOUSE_RIGHT_BUTTON,
    MOUSE_MOVE_LEFT,
    MOUSE_MOVE_RIGHT,
    MOUSE_MOVE_UP,
    MOUSE_MOVE_DOWN,
    MOUSE_SCROLL_UP,
    MOUSE_SCROLL_DOWN,
    KEY_UP,
    KEY_RIGHT,
    KEY_DOWN,
    KEY_LEFT,
    KEY_ENTER,
    KEY_ESC,
    KEY_BKSP,
    KEY_DEL,
    KEY_TAB,
    KEY_SPACE,
    KEY_CTRL_L,
    KEY_CTRL_R,
    KEY_SHIFT_L,
    KEY_SHIFT_R,
    KEY_ALT_L,
    KEY_ALT_R,
    KEY_HOME,
    KEY_PRTSCN,
    KEY_PG_UP,
    KEY_PG_DOWN,
    KEY_CAPS_LOCK,
    KEY_NUM_LOCK,
    KEY_0,
    KEY_1,
    KEY_2,
    KEY_3,
    KEY_4,
    KEY_5,
    KEY_6,
    KEY_7,
    KEY_8,
    KEY_9,
    KEY_A,
    KEY_B,
    KEY_C,
    KEY_D,
    KEY_E,
    KEY_F,
    KEY_G,
    KEY_H,
    KEY_I,
    KEY_J,
    KEY_K,
    KEY_L,
    KEY_M,
    KEY_N,
    KEY_O,
    KEY_P,
    KEY_Q,
    KEY_R,
    KEY_S,
    KEY_T,
    KEY_U,
    KEY_V,
    KEY_W,
    KEY_X,
    KEY_Y,
    KEY_Z,
    KEY_BRACKET_LEFT,
    KEY_BRACKET_RIGHT,
    KEY_BACKSLASH,
    KEY_SLASH,
    KEY_SEMICOLON,
    KEY_COMMA,
    KEY_PERIOD,
    KEY_APOSTROPHE,
    KEY_KP_ADD,
    KEY_MINUS,
    KEY_F1,
    KEY_F2,
    KEY_F3,
    KEY_F4,
    KEY_F5,
    KEY_F6,
    KEY_F7,
    KEY_F8,
    KEY_F9,
    KEY_F10,
    KEY_F11,
    KEY_F12,
    KEY_KP_0,
    KEY_KP_1,
    KEY_KP_2,
    KEY_KP_3,
    KEY_KP_4,
    KEY_KP_5,
    KEY_KP_6,
    KEY_KP_7,
    KEY_KP_8,
    KEY_KP_9,
    GAMEPAD_BUTTON_A,
    GAMEPAD_BUTTON_B,
    GAMEPAD_BUTTON_X,
    GAMEPAD_BUTTON_Y,
    GAMEPAD_BUTTON_L1,
    GAMEPAD_BUTTON_R1,
    GAMEPAD_BUTTON_SELECT,
    GAMEPAD_BUTTON_START,
    GAMEPAD_BUTTON_L3,
    GAMEPAD_BUTTON_R3,
    GAMEPAD_BUTTON_L2,
    GAMEPAD_BUTTON_R2,
    GAMEPAD_LEFT_THUMB_UP,
    GAMEPAD_LEFT_THUMB_RIGHT,
    GAMEPAD_LEFT_THUMB_DOWN,
    GAMEPAD_LEFT_THUMB_LEFT,
    GAMEPAD_RIGHT_THUMB_UP,
    GAMEPAD_RIGHT_THUMB_RIGHT,
    GAMEPAD_RIGHT_THUMB_DOWN,
    GAMEPAD_RIGHT_THUMB_LEFT,
    GAMEPAD_DPAD_UP,
    GAMEPAD_DPAD_RIGHT,
    GAMEPAD_DPAD_DOWN,
    GAMEPAD_DPAD_LEFT;

    val keycode: XKeycode by lazy {
        val nameToFind = when (this) {
            KEY_PG_UP -> "KEY_PRIOR"
            KEY_PG_DOWN -> "KEY_NEXT"
            else -> name
        }
        try {
            XKeycode.valueOf(nameToFind)
        } catch (e: IllegalArgumentException) {
            XKeycode.KEY_NONE
        }
    }

    @NonNull
    override fun toString(): String {
        return when (this) {
            KEY_SHIFT_L -> "L SHIFT"
            KEY_SHIFT_R -> "R SHIFT"
            KEY_CTRL_L -> "L CTRL"
            KEY_CTRL_R -> "R CTRL"
            KEY_ALT_L -> "L ALT"
            KEY_ALT_R -> "R ALT"
            KEY_BRACKET_LEFT -> "["
            KEY_BRACKET_RIGHT -> "]"
            KEY_BACKSLASH -> "\\"
            KEY_SLASH -> "/"
            KEY_SEMICOLON -> ";"
            KEY_COMMA -> ","
            KEY_PERIOD -> "."
            KEY_APOSTROPHE -> "'"
            KEY_MINUS -> "-"
            KEY_KP_ADD -> "+"
            else -> super.toString().replace(Regex("^(MOUSE_)|(KEY_)|(GAMEPAD_)"), "").replace("KP_", "NUMPAD_").replace("_", " ")
        }
    }

    fun getPointerButton(): Pointer.Button? {
        return when (this) {
            MOUSE_LEFT_BUTTON -> Pointer.Button.BUTTON_LEFT
            MOUSE_MIDDLE_BUTTON -> Pointer.Button.BUTTON_MIDDLE
            MOUSE_RIGHT_BUTTON -> Pointer.Button.BUTTON_RIGHT
            MOUSE_SCROLL_UP -> Pointer.Button.BUTTON_SCROLL_UP
            MOUSE_SCROLL_DOWN -> Pointer.Button.BUTTON_SCROLL_DOWN
            else -> null
        }
    }

    fun isMouse(): Boolean {
        return name.startsWith("MOUSE_")
    }

    fun isKeyboard(): Boolean {
        return name.startsWith("KEY_") || this == NONE
    }

    fun isGamepad(): Boolean {
        return name.startsWith("GAMEPAD_")
    }

    fun isMouseMove(): Boolean {
        return this == MOUSE_MOVE_UP || this == MOUSE_MOVE_RIGHT || this == MOUSE_MOVE_DOWN || this == MOUSE_MOVE_LEFT
    }

    companion object {
        fun fromString(name: String): Binding {
            return when (name) {
                "KEY_CTRL" -> KEY_CTRL_L
                "KEY_SHIFT" -> KEY_SHIFT_L
                "KEY_ALT" -> KEY_ALT_L
                else -> valueOf(name)
            }
        }

        fun mouseBindingLabels(): Array<String> {
            return entries.filter { it.isMouse() }.map { it.toString() }.toTypedArray()
        }

        fun keyboardBindingLabels(): Array<String> {
            return entries.filter { it.isKeyboard() }.map { it.toString() }.toTypedArray()
        }

        fun gamepadBindingLabels(): Array<String> {
            return entries.filter { it.isGamepad() }.map { it.toString() }.toTypedArray()
        }

        fun mouseBindingValues(): Array<Binding> {
            return entries.filter { it.isMouse() }.toTypedArray()
        }

        fun keyboardBindingValues(): Array<Binding> {
            return entries.filter { it.isKeyboard() }.toTypedArray()
        }

        fun gamepadBindingValues(): Array<Binding> {
            return entries.filter { it.isGamepad() }.toTypedArray()
        }
    }
}