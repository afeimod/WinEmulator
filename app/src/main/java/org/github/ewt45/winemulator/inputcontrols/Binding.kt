package org.github.ewt45.winemulator.inputcontrols

/**
 * Binding types for control elements
 */
enum class Binding(
    val keycode: Int = 0,
    val isKeyboard: Boolean = false,
    val isMouse: Boolean = false,
    val isGamepad: Boolean = false
) {
    // Special bindings
    NONE(),

    // Keyboard bindings - Main keys
    KEY_ESCAPE(1, true),
    KEY_F1(59, true),
    KEY_F2(60, true),
    KEY_F3(61, true),
    KEY_F4(62, true),
    KEY_F5(63, true),
    KEY_F6(64, true),
    KEY_F7(65, true),
    KEY_F8(66, true),
    KEY_F9(67, true),
    KEY_F10(68, true),
    KEY_F11(69, true),
    KEY_F12(70, true),

    KEY_GRAVE(41, true),
    KEY_1(2, true),
    KEY_2(3, true),
    KEY_3(4, true),
    KEY_4(5, true),
    KEY_5(6, true),
    KEY_6(7, true),
    KEY_7(8, true),
    KEY_8(9, true),
    KEY_9(10, true),
    KEY_0(11, true),
    KEY_MINUS(12, true),
    KEY_EQUALS(13, true),
    KEY_BACKSPACE(14, true),

    KEY_TAB(15, true),
    KEY_Q(16, true),
    KEY_W(17, true),
    KEY_E(18, true),
    KEY_R(19, true),
    KEY_T(20, true),
    KEY_Y(21, true),
    KEY_U(22, true),
    KEY_I(23, true),
    KEY_O(24, true),
    KEY_P(25, true),
    KEY_BRACKET_LEFT(26, true),
    KEY_BRACKET_RIGHT(27, true),
    KEY_BACKSLASH(43, true),

    KEY_CAPITAL(58, true),
    KEY_A(30, true),
    KEY_S(31, true),
    KEY_D(32, true),
    KEY_F(33, true),
    KEY_G(34, true),
    KEY_H(35, true),
    KEY_J(36, true),
    KEY_K(37, true),
    KEY_L(38, true),
    KEY_SEMICOLON(39, true),
    KEY_APOSTROPHE(40, true),
    KEY_ENTER(28, true),

    KEY_LSHIFT(42, true),
    KEY_Z(44, true),
    KEY_X(45, true),
    KEY_C(46, true),
    KEY_V(47, true),
    KEY_B(48, true),
    KEY_N(49, true),
    KEY_M(50, true),
    KEY_COMMA(51, true),
    KEY_PERIOD(52, true),
    KEY_SLASH(53, true),
    KEY_RSHIFT(54, true),

    KEY_LCONTROL(29, true),
    KEY_LWIN(125, true),
    KEY_LMENU(56, true),
    KEY_SPACE(57, true),
    KEY_RMENU(126, true),
    KEY_RWIN(127, true),
    KEY_RCONTROL(157, true),

    // Numpad
    NUMPAD_0(82, true),
    NUMPAD_1(79, true),
    NUMPAD_2(80, true),
    NUMPAD_3(81, true),
    NUMPAD_4(75, true),
    NUMPAD_5(76, true),
    NUMPAD_6(77, true),
    NUMPAD_7(71, true),
    NUMPAD_8(72, true),
    NUMPAD_9(73, true),
    NUMPAD_DECIMAL(83, true),
    NUMPAD_DIVIDE(84, true),
    NUMPAD_MULTIPLY(85, true),
    NUMPAD_MINUS(86, true),
    NUMPAD_PLUS(87, true),
    NUMPAD_EQUAL(117, true),
    NUMPAD_ENTER(156, true),

    // Navigation keys
    KEY_UP(103, true),
    KEY_DOWN(108, true),
    KEY_LEFT(105, true),
    KEY_RIGHT(106, true),
    KEY_INSERT(110, true),
    KEY_HOME(102, true),
    KEY_PAGEUP(104, true),
    KEY_DELETE(111, true),
    KEY_END(107, true),
    KEY_PAGEDOWN(109, true),

    // Mouse bindings
    MOUSE_LEFT_BUTTON(0, false, true),
    MOUSE_RIGHT_BUTTON(0, false, true),
    MOUSE_MIDDLE_BUTTON(0, false, true),
    MOUSE_MOVE_UP(0, false, true),
    MOUSE_MOVE_DOWN(0, false, true),
    MOUSE_MOVE_LEFT(0, false, true),
    MOUSE_MOVE_RIGHT(0, false, true),
    MOUSE_SCROLL_UP(0, false, true),
    MOUSE_SCROLL_DOWN(0, false, true),
    MOUSE_LEFT_RIGHT(0, false, true),
    MOUSE_TOUCHMODE_SWITCH(0, false, true),

    // Gamepad bindings
    GAMEPAD_BUTTON_A(0, false, false, true),
    GAMEPAD_BUTTON_B(0, false, false, true),
    GAMEPAD_BUTTON_X(0, false, false, true),
    GAMEPAD_BUTTON_Y(0, false, false, true),
    GAMEPAD_BUTTON_L1(0, false, false, true),
    GAMEPAD_BUTTON_R1(0, false, false, true),
    GAMEPAD_BUTTON_L2(0, false, false, true),
    GAMEPAD_BUTTON_R2(0, false, false, true),
    GAMEPAD_BUTTON_THUMBL(0, false, false, true),
    GAMEPAD_BUTTON_THUMBR(0, false, false, true),
    GAMEPAD_BUTTON_START(0, false, false, true),
    GAMEPAD_BUTTON_SELECT(0, false, false, true),
    GAMEPAD_BUTTON_HOME(0, false, false, true),
    GAMEPAD_DPAD_UP(0, false, false, true),
    GAMEPAD_DPAD_DOWN(0, false, false, true),
    GAMEPAD_DPAD_LEFT(0, false, false, true),
    GAMEPAD_DPAD_RIGHT(0, false, false, true),
    GAMEPAD_LEFT_THUMB_UP(0, false, false, true),
    GAMEPAD_LEFT_THUMB_DOWN(0, false, false, true),
    GAMEPAD_LEFT_THUMB_LEFT(0, false, false, true),
    GAMEPAD_LEFT_THUMB_RIGHT(0, false, false, true),
    GAMEPAD_RIGHT_THUMB_UP(0, false, false, true),
    GAMEPAD_RIGHT_THUMB_DOWN(0, false, false, true),
    GAMEPAD_RIGHT_THUMB_LEFT(0, false, false, true),
    GAMEPAD_RIGHT_THUMB_RIGHT(0, false, false, true);

    companion object {
        fun fromString(name: String): Binding {
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                NONE
            }
        }

        fun keyboardBindings(): List<Binding> = entries.filter { it.isKeyboard }

        fun mouseBindings(): List<Binding> = entries.filter { it.isMouse }

        fun gamepadBindings(): List<Binding> = entries.filter { it.isGamepad }

        fun keyboardBindingLabels(): Array<String> =
            keyboardBindings().map { it.name.replace("KEY_", "").replace("NUMPAD_", "NP") }.toTypedArray()

        fun keyboardBindingValues(): Array<Binding> =
            keyboardBindings().toTypedArray()

        fun mouseBindingLabels(): Array<String> =
            mouseBindings().map {
                it.name.replace("MOUSE_", "")
                    .replace("_BUTTON", " BTN")
                    .replace("_MOVE_", " ")
                    .replace("_", " ")
            }.toTypedArray()

        fun mouseBindingValues(): Array<Binding> =
            mouseBindings().toTypedArray()

        fun gamepadBindingLabels(): Array<String> =
            gamepadBindings().map {
                it.name.replace("GAMEPAD_", "")
                    .replace("BUTTON_", "")
                    .replace("THUMB", "T")
                    .replace("LEFT_", "L")
                    .replace("RIGHT_", "R")
                    .replace("DPAD", "D")
            }.toTypedArray()

        fun gamepadBindingValues(): Array<Binding> =
            gamepadBindings().toTypedArray()
    }

    fun isMouseMove(): Boolean = this in listOf(MOUSE_MOVE_UP, MOUSE_MOVE_DOWN, MOUSE_MOVE_LEFT, MOUSE_MOVE_RIGHT)
}
