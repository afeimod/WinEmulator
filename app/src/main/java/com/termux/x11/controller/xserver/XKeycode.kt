package com.termux.x11.controller.xserver

/**
 * X11 keycode enumeration for termux-x11 compatibility.
 * Maps Android keycodes to X11 keycodes.
 */
enum class XKeycode(val id: Int) {
    KEY_NONE(0),
    KEY_ESCAPE(9),
    KEY_1(10),
    KEY_2(11),
    KEY_3(12),
    KEY_4(13),
    KEY_5(14),
    KEY_6(15),
    KEY_7(16),
    KEY_8(17),
    KEY_9(18),
    KEY_0(19),
    KEY_MINUS(20),
    KEY_EQUAL(21),
    KEY_BACKSPACE(22),
    KEY_TAB(23),
    KEY_Q(24),
    KEY_W(25),
    KEY_E(26),
    KEY_R(27),
    KEY_T(28),
    KEY_Y(29),
    KEY_U(30),
    KEY_I(31),
    KEY_O(32),
    KEY_P(33),
    KEY_LEFTBRACE(34),
    KEY_RIGHTBRACE(35),
    KEY_ENTER(36),
    KEY_LEFTCTRL(37),
    KEY_A(38),
    KEY_S(39),
    KEY_D(40),
    KEY_F(41),
    KEY_G(42),
    KEY_H(43),
    KEY_J(44),
    KEY_K(45),
    KEY_L(46),
    KEY_SEMICOLON(47),
    KEY_APOSTROPHE(48),
    KEY_GRAVE(49),
    KEY_LEFTSHIFT(50),
    KEY_BACKSLASH(51),
    KEY_Z(52),
    KEY_X(53),
    KEY_C(54),
    KEY_V(55),
    KEY_B(56),
    KEY_N(57),
    KEY_M(58),
    KEY_COMMA(59),
    KEY_DOT(60),
    KEY_SLASH(61),
    KEY_RIGHTSHIFT(62),
    KEY_KPASTERISK(63),
    KEY_LEFTALT(64),
    KEY_SPACE(65),
    KEY_CAPSLOCK(66),
    KEY_F1(67),
    KEY_F2(68),
    KEY_F3(69),
    KEY_F4(70),
    KEY_F5(71),
    KEY_F6(72),
    KEY_F7(73),
    KEY_F8(74),
    KEY_F9(75),
    KEY_F10(76),
    KEY_NUMLOCK(77),
    KEY_SCROLLLOCK(78),
    KEY_KP7(79),
    KEY_KP8(80),
    KEY_KP9(81),
    KEY_KPMINUS(82),
    KEY_KP4(83),
    KEY_KP5(84),
    KEY_KP6(85),
    KEY_KPPLUS(86),
    KEY_KP1(87),
    KEY_KP2(88),
    KEY_KP3(89),
    KEY_KP0(90),
    KEY_KPDOT(91),
    KEY_ZENKAKUHANKAKU(92),
    KEY_102ND(93),
    KEY_F11(95),
    KEY_F12(96),
    KEY_RO(97),
    KEY_KATAKANA(98),
    KEY_HIRAGANA(99),
    KEY_HENKAN(100),
    KEY_KATAKANAHIRAGANA(101),
    KEY_MUHENKAN(102),
    KEY_KPJPCOMMA(103),
    KEY_KPENTER(104),
    KEY_RIGHTCTRL(105),
    KEY_KPSLASH(106),
    KEY_SYSRQ(107),
    KEY_RIGHTALT(108),
    KEY_LINEFEED(109),
    KEY_HOME(110),
    KEY_UP(111),
    KEY_PAGEUP(112),
    KEY_LEFT(113),
    KEY_RIGHT(114),
    KEY_END(115),
    KEY_DOWN(116),
    KEY_PAGEDOWN(117),
    KEY_INSERT(118),
    KEY_DELETE(119),
    KEY_MIN(120),
    KEY_MACRO(121),
    KEY_MUTE(123),
    KEY_VOLUMEDOWN(124),
    KEY_VOLUMEUP(125),
    KEY_POWER(127),
    KEY_KPEQUAL(128),
    KEY_KPPLUSMINUS(129),
    KEY_PAUSE(130),
    KEY_SCALE(131),
    KEY_KPCOMMA(132),
    KEY_HANGEUL(133),
    KEY_HANJA(134),
    KEY_YEN(135),
    KEY_LEFTMETA(137),
    KEY_RIGHTMETA(138),
    KEY_COMPOSE(139),
    KEY_STOP(128),
    KEY_AGAIN(129),
    KEY_PROPS(130),
    KEY_UNDO(131),
    KEY_FRONT(132),
    KEY_COPY(133),
    KEY_OPEN(134),
    KEY_PASTE(135),
    KEY_FIND(136),
    KEY_CUT(137),
    KEY_HELP(138),
    KEY_MENU(139),
    KEY_CALC(140),
    KEY_SETUP(141),
    KEY_SLEEP(142),
    KEY_WAKEUP(143),
    KEY_FILE(144),
    KEY_SENDFILE(145),
    KEY_DELETEFILE(146),
    KEY_XFER(147),
    KEY_PROG1(148),
    KEY_PROG2(149),
    KEY_WWW(150),
    KEY_MSDOS(151),
    KEY_COFFEE(152),
    KEY_DIRECTION(153),
    KEY_CYCLEWINDOWS(154),
    KEY_MAIL(155),
    KEY_BOOKMARKS(156),
    KEY_COMPUTER(157),
    KEY_BACK(158),
    KEY_FORWARD(159),
    KEY_CLOSECD(160),
    KEY_EJECTCD(161),
    KEY_EJECTCLOSECD(162),
    KEY_NEXTSONG(163),
    KEY_PLAYPAUSE(164),
    KEY_PREVIOUSSONG(165),
    KEY_STOPCD(166),
    KEY_REFRESH(167),
    KEY_F13(191),
    KEY_F14(192),
    KEY_F15(193),
    KEY_F16(194),
    KEY_F17(195),
    KEY_F18(196),
    KEY_F19(197),
    KEY_F20(198),
    KEY_F21(199),
    KEY_F22(200),
    KEY_F23(201),
    KEY_F24(202),
    KEY_PRIOR(112),  // Alias for KEY_PAGEUP
    KEY_NEXT(117);   // Alias for KEY_PAGEDOWN

    companion object {
        /**
         * Get XKeycode from Android keycode
         */
        fun fromAndroidKeycode(keycode: Int): XKeycode? {
            return when (keycode) {
                android.view.KeyEvent.KEYCODE_ESCAPE -> KEY_ESCAPE
                android.view.KeyEvent.KEYCODE_1 -> KEY_1
                android.view.KeyEvent.KEYCODE_2 -> KEY_2
                android.view.KeyEvent.KEYCODE_3 -> KEY_3
                android.view.KeyEvent.KEYCODE_4 -> KEY_4
                android.view.KeyEvent.KEYCODE_5 -> KEY_5
                android.view.KeyEvent.KEYCODE_6 -> KEY_6
                android.view.KeyEvent.KEYCODE_7 -> KEY_7
                android.view.KeyEvent.KEYCODE_8 -> KEY_8
                android.view.KeyEvent.KEYCODE_9 -> KEY_9
                android.view.KeyEvent.KEYCODE_0 -> KEY_0
                android.view.KeyEvent.KEYCODE_MINUS -> KEY_MINUS
                android.view.KeyEvent.KEYCODE_EQUALS -> KEY_EQUAL
                android.view.KeyEvent.KEYCODE_DEL -> KEY_BACKSPACE
                android.view.KeyEvent.KEYCODE_TAB -> KEY_TAB
                android.view.KeyEvent.KEYCODE_Q -> KEY_Q
                android.view.KeyEvent.KEYCODE_W -> KEY_W
                android.view.KeyEvent.KEYCODE_E -> KEY_E
                android.view.KeyEvent.KEYCODE_R -> KEY_R
                android.view.KeyEvent.KEYCODE_T -> KEY_T
                android.view.KeyEvent.KEYCODE_Y -> KEY_Y
                android.view.KeyEvent.KEYCODE_U -> KEY_U
                android.view.KeyEvent.KEYCODE_I -> KEY_I
                android.view.KeyEvent.KEYCODE_O -> KEY_O
                android.view.KeyEvent.KEYCODE_P -> KEY_P
                android.view.KeyEvent.KEYCODE_LEFT_BRACKET -> KEY_LEFTBRACE
                android.view.KeyEvent.KEYCODE_RIGHT_BRACKET -> KEY_RIGHTBRACE
                android.view.KeyEvent.KEYCODE_ENTER -> KEY_ENTER
                android.view.KeyEvent.KEYCODE_CTRL_LEFT -> KEY_LEFTCTRL
                android.view.KeyEvent.KEYCODE_A -> KEY_A
                android.view.KeyEvent.KEYCODE_S -> KEY_S
                android.view.KeyEvent.KEYCODE_D -> KEY_D
                android.view.KeyEvent.KEYCODE_F -> KEY_F
                android.view.KeyEvent.KEYCODE_G -> KEY_G
                android.view.KeyEvent.KEYCODE_H -> KEY_H
                android.view.KeyEvent.KEYCODE_J -> KEY_J
                android.view.KeyEvent.KEYCODE_K -> KEY_K
                android.view.KeyEvent.KEYCODE_L -> KEY_L
                android.view.KeyEvent.KEYCODE_SEMICOLON -> KEY_SEMICOLON
                android.view.KeyEvent.KEYCODE_APOSTROPHE -> KEY_APOSTROPHE
                android.view.KeyEvent.KEYCODE_GRAVE -> KEY_GRAVE
                android.view.KeyEvent.KEYCODE_SHIFT_LEFT -> KEY_LEFTSHIFT
                android.view.KeyEvent.KEYCODE_BACKSLASH -> KEY_BACKSLASH
                android.view.KeyEvent.KEYCODE_Z -> KEY_Z
                android.view.KeyEvent.KEYCODE_X -> KEY_X
                android.view.KeyEvent.KEYCODE_C -> KEY_C
                android.view.KeyEvent.KEYCODE_V -> KEY_V
                android.view.KeyEvent.KEYCODE_B -> KEY_B
                android.view.KeyEvent.KEYCODE_N -> KEY_N
                android.view.KeyEvent.KEYCODE_M -> KEY_M
                android.view.KeyEvent.KEYCODE_COMMA -> KEY_COMMA
                android.view.KeyEvent.KEYCODE_PERIOD -> KEY_DOT
                android.view.KeyEvent.KEYCODE_SLASH -> KEY_SLASH
                android.view.KeyEvent.KEYCODE_SHIFT_RIGHT -> KEY_RIGHTSHIFT
                android.view.KeyEvent.KEYCODE_ALT_LEFT -> KEY_LEFTALT
                android.view.KeyEvent.KEYCODE_SPACE -> KEY_SPACE
                android.view.KeyEvent.KEYCODE_CAPS_LOCK -> KEY_CAPSLOCK
                android.view.KeyEvent.KEYCODE_F1 -> KEY_F1
                android.view.KeyEvent.KEYCODE_F2 -> KEY_F2
                android.view.KeyEvent.KEYCODE_F3 -> KEY_F3
                android.view.KeyEvent.KEYCODE_F4 -> KEY_F4
                android.view.KeyEvent.KEYCODE_F5 -> KEY_F5
                android.view.KeyEvent.KEYCODE_F6 -> KEY_F6
                android.view.KeyEvent.KEYCODE_F7 -> KEY_F7
                android.view.KeyEvent.KEYCODE_F8 -> KEY_F8
                android.view.KeyEvent.KEYCODE_F9 -> KEY_F9
                android.view.KeyEvent.KEYCODE_F10 -> KEY_F10
                android.view.KeyEvent.KEYCODE_NUM_LOCK -> KEY_NUMLOCK
                android.view.KeyEvent.KEYCODE_SCROLL_LOCK -> KEY_SCROLLLOCK
                android.view.KeyEvent.KEYCODE_DPAD_UP -> KEY_UP
                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> KEY_DOWN
                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> KEY_LEFT
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> KEY_RIGHT
                android.view.KeyEvent.KEYCODE_MOVE_HOME -> KEY_HOME
                android.view.KeyEvent.KEYCODE_MOVE_END -> KEY_END
                android.view.KeyEvent.KEYCODE_INSERT -> KEY_INSERT
                android.view.KeyEvent.KEYCODE_FORWARD_DEL -> KEY_DELETE
                android.view.KeyEvent.KEYCODE_PAGE_UP -> KEY_PAGEUP
                android.view.KeyEvent.KEYCODE_PAGE_DOWN -> KEY_PAGEDOWN
                android.view.KeyEvent.KEYCODE_CTRL_RIGHT -> KEY_RIGHTCTRL
                android.view.KeyEvent.KEYCODE_ALT_RIGHT -> KEY_RIGHTALT
                android.view.KeyEvent.KEYCODE_MUTE -> KEY_MUTE
                android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> KEY_VOLUMEDOWN
                android.view.KeyEvent.KEYCODE_VOLUME_UP -> KEY_VOLUMEUP
                android.view.KeyEvent.KEYCODE_POWER -> KEY_POWER
                android.view.KeyEvent.KEYCODE_NUMPAD_0 -> KEY_KP0
                android.view.KeyEvent.KEYCODE_NUMPAD_1 -> KEY_KP1
                android.view.KeyEvent.KEYCODE_NUMPAD_2 -> KEY_KP2
                android.view.KeyEvent.KEYCODE_NUMPAD_3 -> KEY_KP3
                android.view.KeyEvent.KEYCODE_NUMPAD_4 -> KEY_KP4
                android.view.KeyEvent.KEYCODE_NUMPAD_5 -> KEY_KP5
                android.view.KeyEvent.KEYCODE_NUMPAD_6 -> KEY_KP6
                android.view.KeyEvent.KEYCODE_NUMPAD_7 -> KEY_KP7
                android.view.KeyEvent.KEYCODE_NUMPAD_8 -> KEY_KP8
                android.view.KeyEvent.KEYCODE_NUMPAD_9 -> KEY_KP9
                android.view.KeyEvent.KEYCODE_NUMPAD_DIVIDE -> KEY_KPSLASH
                android.view.KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> KEY_KPASTERISK
                android.view.KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> KEY_KPMINUS
                android.view.KeyEvent.KEYCODE_NUMPAD_ADD -> KEY_KPPLUS
                android.view.KeyEvent.KEYCODE_NUMPAD_DOT -> KEY_KPDOT
                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> KEY_KPENTER
                else -> null
            }
        }
    }
}
