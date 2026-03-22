package org.github.ewt45.winemulator.inputcontrols

/**
 * Binding types for control elements
 * 按键绑定类型
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
    // 常用按键 - 与配置文件兼容的名称
    KEY_ESC(1, true),
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
    KEY_BKSP(14, true),
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
    KEY_CAPS_LOCK(58, true),
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

    KEY_SHIFT_L(42, true),
    KEY_LSHIFT(42, true),
    KEY_SHIFT_R(54, true),
    KEY_RSHIFT(54, true),
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

    // Control keys - 兼容多种命名方式
    KEY_CTRL_L(29, true),
    KEY_LCTRL(29, true),
    KEY_LCONTROL(29, true),
    KEY_CTRL_R(157, true),
    KEY_RCTRL(157, true),
    KEY_RCONTROL(157, true),

    KEY_LWIN(125, true),
    KEY_LMENU(56, true),
    KEY_LALT(56, true),
    KEY_ALT_L(56, true),
    KEY_SPACE(57, true),
    KEY_RMENU(126, true),
    KEY_RALT(126, true),
    KEY_ALT_R(126, true),
    KEY_RWIN(127, true),

    // Navigation keys
    KEY_UP(103, true),
    KEY_DOWN(108, true),
    KEY_LEFT(105, true),
    KEY_RIGHT(106, true),
    KEY_INSERT(110, true),
    KEY_HOME(102, true),
    KEY_END(107, true),
    KEY_PGUP(104, true),
    KEY_PAGEUP(104, true),
    KEY_PGDN(109, true),
    KEY_PAGEDOWN(109, true),
    KEY_DELETE(111, true),
    KEY_DEL(111, true),
    KEY_PRTSCN(127, true),
    KEY_PRINT(127, true),
    KEY_SCROLL_LOCK(70, true),
    KEY_PAUSE(197, true),

    // Numpad - 兼容多种命名方式
    NUMPAD_0(82, true),
    KEY_KP_0(82, true),
    NUMPAD_1(79, true),
    KEY_KP_1(79, true),
    NUMPAD_2(80, true),
    KEY_KP_2(80, true),
    NUMPAD_3(81, true),
    KEY_KP_3(81, true),
    NUMPAD_4(75, true),
    KEY_KP_4(75, true),
    NUMPAD_5(76, true),
    KEY_KP_5(76, true),
    NUMPAD_6(77, true),
    KEY_KP_6(77, true),
    NUMPAD_7(71, true),
    KEY_KP_7(71, true),
    NUMPAD_8(72, true),
    KEY_KP_8(72, true),
    NUMPAD_9(73, true),
    KEY_KP_9(73, true),
    NUMPAD_DECIMAL(83, true),
    KEY_KP_DECIMAL(83, true),
    NUMPAD_DIVIDE(84, true),
    KEY_KP_DIVIDE(84, true),
    NUMPAD_MULTIPLY(85, true),
    KEY_KP_MULTIPLY(85, true),
    NUMPAD_MINUS(86, true),
    KEY_KP_SUBTRACT(86, true),
    NUMPAD_PLUS(87, true),
    KEY_KP_ADD(87, true),
    NUMPAD_ENTER(156, true),
    KEY_KP_ENTER(156, true),

    // Additional special keys
    KEY_NUM_LOCK(69, true),
    KEY_NUMLOCK(69, true),
    KEY_SCROLL(151, true),

    // Mouse bindings - 鼠标绑定
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

    // Gamepad bindings - 手柄绑定
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
        /**
         * 从字符串解析Binding，支持多种命名变体
         */
        fun fromString(name: String): Binding {
            return try {
                // 直接匹配
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                // 尝试常见的别名映射
                when (name) {
                    "KEY_ESC" -> KEY_ESC
                    "KEY_BKSP", "KEY_BACKSPACE" -> KEY_BKSP
                    "KEY_SHIFT", "KEY_LSHIFT" -> KEY_SHIFT_L
                    "KEY_RSHIFT" -> KEY_SHIFT_R
                    "KEY_CTRL", "KEY_LCTRL", "KEY_LCONTROL" -> KEY_CTRL_L
                    "KEY_RCTRL", "KEY_RCONTROL" -> KEY_CTRL_R
                    "KEY_ALT", "KEY_LALT" -> KEY_ALT_L
                    "KEY_RALT" -> KEY_ALT_R
                    "KEY_DELETE" -> KEY_DEL
                    "KEY_PAGEUP", "KEY_PG_UP" -> KEY_PGUP
                    "KEY_PAGEDOWN", "KEY_PG_DOWN" -> KEY_PGDN
                    "KEY_PRINT" -> KEY_PRTSCN
                    "KEY_CAPS" -> KEY_CAPITAL
                    "KEY_NUMLOCK" -> KEY_NUM_LOCK
                    "KEY_SCROLL" -> KEY_SCROLL_LOCK
                    "KEY_KP0", "NUMPAD_0" -> NUMPAD_0
                    "KEY_KP1", "NUMPAD_1" -> NUMPAD_1
                    "KEY_KP2", "NUMPAD_2" -> NUMPAD_2
                    "KEY_KP3", "NUMPAD_3" -> NUMPAD_3
                    "KEY_KP4", "NUMPAD_4" -> NUMPAD_4
                    "KEY_KP5", "NUMPAD_5" -> NUMPAD_5
                    "KEY_KP6", "NUMPAD_6" -> NUMPAD_6
                    "KEY_KP7", "NUMPAD_7" -> NUMPAD_7
                    "KEY_KP8", "NUMPAD_8" -> NUMPAD_8
                    "KEY_KP9", "NUMPAD_9" -> NUMPAD_9
                    "KEY_KP_DIVIDE" -> NUMPAD_DIVIDE
                    "KEY_KP_MULTIPLY" -> NUMPAD_MULTIPLY
                    "KEY_KP_SUBTRACT" -> NUMPAD_MINUS
                    "KEY_KP_ADD" -> NUMPAD_PLUS
                    "KEY_KP_DECIMAL" -> NUMPAD_DECIMAL
                    "KEY_KP_ENTER" -> NUMPAD_ENTER
                    else -> NONE
                }
            }
        }

        fun keyboardBindings(): List<Binding> = entries.filter { it.isKeyboard }

        fun mouseBindings(): List<Binding> = entries.filter { it.isMouse }

        fun gamepadBindings(): List<Binding> = entries.filter { it.isGamepad }

        fun keyboardBindingLabels(): Array<String> =
            keyboardBindings().map { binding ->
                // 简化标签显示
                binding.name
                    .replace("KEY_", "")
                    .replace("NUMPAD_", "NP")
                    .replace("L", "L ")
                    .replace("R", "R ")
                    .replace("CTRL", "CTRL")
                    .replace("SHIFT", "SHIFT")
                    .replace("ALT", "ALT")
            }.toTypedArray()

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
