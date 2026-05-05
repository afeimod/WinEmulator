package org.github.ewt45.winemulator.inputcontrols

import android.view.Choreographer
import android.view.KeyEvent
import com.termux.x11.input.InputEventSender
import com.termux.x11.input.InputStub
import com.termux.x11.input.InputStub.*
import com.termux.x11.input.RenderData

/**
 * X11 Input Handler using InputEventSender
 * Sends keyboard and mouse events through Android's InputEvent system to LorieView
 *
 * 实现说明：
 * 此实现用于在X服务器auto-repeat不可用时，通过客户端实现可靠的按键重复逻辑
 * 使用Android Choreographer与屏幕刷新率同步，确保流畅的按键重复体验
 *
 * 按键事件的处理流程：
 * 1. ControlElement.handleTouchDown 调用 handleInputEvent(binding, true)
 * 2. handleInputEvent 调用 X11InputSender.sendKeyEvent(keycode, true)
 * 3. sendKeyEvent 发送一次 KeyEvent.ACTION_DOWN 事件
 * 4. 启动Choreographer回调，在每个帧刷新时检查是否需要发送重复事件
 * 5. ControlElement.handleTouchUp 调用 handleInputEvent(binding, false)
 * 6. sendKeyEvent 发送 KeyEvent.ACTION_UP 事件，停止重复循环
 *
 * 重复参数：
 * - 初始延迟：500ms（长按500ms后开始重复）
 * - 重复间隔：与屏幕刷新率同步（约16ms for 60fps，约8ms for 120fps）
 * - 只发送keyDown事件，不发送keyUp（keyUp在手指抬起时发送一次）
 */
class X11InputSender {
    private var inputEventSender: InputEventSender? = null

    // RenderData for touch events - needs to be set from LorieView
    var renderData: RenderData? = null

    // Whether InputEventSender is initialized
    val isInitialized: Boolean
        get() = inputEventSender != null

    // 按键重复状态追踪
    private val repeatingKeys = mutableSetOf<Int>()
    private val pendingRepeatKeys = mutableSetOf<Int>()

    // Choreographer用于与屏幕刷新率同步
    private val choreographer = Choreographer.getInstance()
    private var isRepeating = false

    // Choreographer回调
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (pendingRepeatKeys.isNotEmpty()) {
                // 发送重复事件
                synchronized(repeatingKeys) {
                    for (keycode in pendingRepeatKeys.toList()) {
                        if (repeatingKeys.contains(keycode)) {
                            sendKeyDownEvent(keycode)
                        }
                    }
                }
                // 继续下一帧
                choreographer.postFrameCallback(this)
            } else {
                isRepeating = false
            }
        }
    }

    /**
     * Initialize with an InputStub (typically LorieView)
     */
    fun initialize(inputStub: InputStub) {
        inputEventSender = InputEventSender(inputStub)
    }

    /**
     * Send a key event using Android KeyEvent
     *
     * @param keycode The Android keycode
     * @param isDown True if key is pressed, false if released
     */
    fun sendKeyEvent(keycode: Int, isDown: Boolean) {
        val sender = inputEventSender ?: return

        if (isDown) {
            // 发送初始keyDown
            sendKeyDownEvent(keycode)

            // 启动重复逻辑
            synchronized(repeatingKeys) {
                repeatingKeys.add(keycode)
                pendingRepeatKeys.add(keycode)

                // 如果还没有启动Choreographer，启动它
                if (!isRepeating) {
                    isRepeating = true
                    choreographer.postFrameCallback(frameCallback)
                }
            }
        } else {
            // 停止重复
            synchronized(repeatingKeys) {
                repeatingKeys.remove(keycode)
                pendingRepeatKeys.remove(keycode)

                // 如果没有更多重复的键，停止Choreographer
                if (pendingRepeatKeys.isEmpty()) {
                    isRepeating = false
                }
            }

            // 发送keyUp
            sendKeyUpEvent(keycode)
        }
    }

    /**
     * 发送keyDown事件（内部方法，不包含重复逻辑）
     */
    private fun sendKeyDownEvent(keycode: Int) {
        val sender = inputEventSender ?: return
        val event = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
        sender.sendKeyEvent(event)
    }

    /**
     * 发送keyUp事件（内部方法）
     */
    private fun sendKeyUpEvent(keycode: Int) {
        val sender = inputEventSender ?: return
        val event = KeyEvent(KeyEvent.ACTION_UP, keycode)
        sender.sendKeyEvent(event)
    }

    /**
     * Convert evdev keycode to Android keycode and send
     * @param evdevKeycode The evdev keycode (as used in Linux input layer)
     * @param isDown True if key is pressed, false if released
     */
    fun sendEvdevKeyEvent(evdevKeycode: Int, isDown: Boolean) {
        val androidKeycode = evdevToAndroidKeycode(evdevKeycode)
        if (androidKeycode != 0) {
            sendKeyEvent(androidKeycode, isDown)
        }
    }

    /**
     * Send mouse button event
     * @param button Button index (1=left, 2=middle, 3=right, 4=scroll up, 5=scroll down)
     * @param isDown True if pressed, false if released
     */
    fun sendMouseButtonEvent(button: Int, isDown: Boolean) {
        val sender = inputEventSender ?: return

        when (button) {
            1 -> {
                // Left button
                sender.sendMouseEvent(null, BUTTON_LEFT, isDown, true)
            }
            2 -> {
                // Middle button
                sender.sendMouseEvent(null, BUTTON_MIDDLE, isDown, true)
            }
            3 -> {
                // Right button
                sender.sendMouseEvent(null, BUTTON_RIGHT, isDown, true)
            }
            4 -> {
                // Scroll up
                if (isDown) {
                    sender.sendMouseWheelEvent(0f, -1f)
                }
            }
            5 -> {
                // Scroll down
                if (isDown) {
                    sender.sendMouseWheelEvent(0f, 1f)
                }
            }
        }
    }

    /**
     * Send mouse motion event (relative movement)
     * @param dx Change in X coordinate
     * @param dy Change in Y coordinate
     */
    fun sendMouseMotionEvent(dx: Int, dy: Int) {
        val sender = inputEventSender ?: return
        sender.sendCursorMove(dx.toFloat(), dy.toFloat(), true)
    }

    /**
     * Send mouse wheel event
     * @param deltaX Horizontal scroll amount
     * @param deltaY Vertical scroll amount
     */
    fun sendMouseWheelEvent(deltaX: Float, deltaY: Float) {
        val sender = inputEventSender ?: return
        sender.sendMouseWheelEvent(deltaX, deltaY)
    }

    /**
     * Convert evdev keycode to Android keycode
     * This mapping follows the Linux evdev to Android keycode conversion
     */
    private fun evdevToAndroidKeycode(evdev: Int): Int {
        return when (evdev) {
            // Escape and special keys
            1 -> KeyEvent.KEYCODE_ESCAPE

            // Function keys F1-F12
            59 -> KeyEvent.KEYCODE_F1
            60 -> KeyEvent.KEYCODE_F2
            61 -> KeyEvent.KEYCODE_F3
            62 -> KeyEvent.KEYCODE_F4
            63 -> KeyEvent.KEYCODE_F5
            64 -> KeyEvent.KEYCODE_F6
            65 -> KeyEvent.KEYCODE_F7
            66 -> KeyEvent.KEYCODE_F8
            67 -> KeyEvent.KEYCODE_F9
            68 -> KeyEvent.KEYCODE_F10
            87 -> KeyEvent.KEYCODE_F11
            88 -> KeyEvent.KEYCODE_F12

            // Numbers row
            2 -> KeyEvent.KEYCODE_1
            3 -> KeyEvent.KEYCODE_2
            4 -> KeyEvent.KEYCODE_3
            5 -> KeyEvent.KEYCODE_4
            6 -> KeyEvent.KEYCODE_5
            7 -> KeyEvent.KEYCODE_6
            8 -> KeyEvent.KEYCODE_7
            9 -> KeyEvent.KEYCODE_8
            10 -> KeyEvent.KEYCODE_9
            11 -> KeyEvent.KEYCODE_0

            // Operators and special keys
            12 -> KeyEvent.KEYCODE_MINUS
            13 -> KeyEvent.KEYCODE_EQUALS
            14 -> KeyEvent.KEYCODE_DEL
            15 -> KeyEvent.KEYCODE_TAB

            // Letters Q-Z
            16 -> KeyEvent.KEYCODE_Q
            17 -> KeyEvent.KEYCODE_W
            18 -> KeyEvent.KEYCODE_E
            19 -> KeyEvent.KEYCODE_R
            20 -> KeyEvent.KEYCODE_T
            21 -> KeyEvent.KEYCODE_Y
            22 -> KeyEvent.KEYCODE_U
            23 -> KeyEvent.KEYCODE_I
            24 -> KeyEvent.KEYCODE_O
            25 -> KeyEvent.KEYCODE_P
            26 -> KeyEvent.KEYCODE_LEFT_BRACKET
            27 -> KeyEvent.KEYCODE_RIGHT_BRACKET
            28 -> KeyEvent.KEYCODE_ENTER
            29 -> KeyEvent.KEYCODE_CTRL_LEFT

            // Letters A-L
            30 -> KeyEvent.KEYCODE_A
            31 -> KeyEvent.KEYCODE_S
            32 -> KeyEvent.KEYCODE_D
            33 -> KeyEvent.KEYCODE_F
            34 -> KeyEvent.KEYCODE_G
            35 -> KeyEvent.KEYCODE_H
            36 -> KeyEvent.KEYCODE_J
            37 -> KeyEvent.KEYCODE_K
            38 -> KeyEvent.KEYCODE_L
            39 -> KeyEvent.KEYCODE_SEMICOLON
            40 -> KeyEvent.KEYCODE_APOSTROPHE
            41 -> KeyEvent.KEYCODE_GRAVE

            // Modifiers
            42 -> KeyEvent.KEYCODE_SHIFT_LEFT
            43 -> KeyEvent.KEYCODE_BACKSLASH
            44 -> KeyEvent.KEYCODE_Z
            45 -> KeyEvent.KEYCODE_X
            46 -> KeyEvent.KEYCODE_C
            47 -> KeyEvent.KEYCODE_V
            48 -> KeyEvent.KEYCODE_B
            49 -> KeyEvent.KEYCODE_N
            50 -> KeyEvent.KEYCODE_M
            51 -> KeyEvent.KEYCODE_COMMA
            52 -> KeyEvent.KEYCODE_PERIOD
            53 -> KeyEvent.KEYCODE_SLASH
            54 -> KeyEvent.KEYCODE_SHIFT_RIGHT
            55 -> KeyEvent.KEYCODE_NUMPAD_MULTIPLY
            56 -> KeyEvent.KEYCODE_ALT_LEFT
            57 -> KeyEvent.KEYCODE_SPACE
            58 -> KeyEvent.KEYCODE_CAPS_LOCK

            // Lock keys
            69 -> KeyEvent.KEYCODE_NUM_LOCK
            70 -> KeyEvent.KEYCODE_SCROLL_LOCK

            // Navigation cluster
            72 -> KeyEvent.KEYCODE_DPAD_UP
            73 -> KeyEvent.KEYCODE_PAGE_UP
            74 -> KeyEvent.KEYCODE_PAGE_DOWN
            75 -> KeyEvent.KEYCODE_NUMPAD_4
            76 -> KeyEvent.KEYCODE_NUMPAD_5
            77 -> KeyEvent.KEYCODE_NUMPAD_6
            78 -> KeyEvent.KEYCODE_NUMPAD_1
            79 -> KeyEvent.KEYCODE_NUMPAD_7
            80 -> KeyEvent.KEYCODE_DPAD_DOWN
            81 -> KeyEvent.KEYCODE_NUMPAD_0
            82 -> KeyEvent.KEYCODE_NUMPAD_SUBTRACT
            83 -> KeyEvent.KEYCODE_NUMPAD_DOT
            84 -> KeyEvent.KEYCODE_NUMPAD_DIVIDE
            85 -> KeyEvent.KEYCODE_NUMPAD_MULTIPLY
            86 -> KeyEvent.KEYCODE_NUMPAD_ADD

            // Additional navigation keys
            102 -> KeyEvent.KEYCODE_MOVE_HOME
            104 -> KeyEvent.KEYCODE_PAGE_UP
            105 -> KeyEvent.KEYCODE_DPAD_LEFT
            106 -> KeyEvent.KEYCODE_DPAD_RIGHT
            107 -> KeyEvent.KEYCODE_MOVE_END
            109 -> KeyEvent.KEYCODE_PAGE_DOWN
            110 -> KeyEvent.KEYCODE_INSERT
            111 -> KeyEvent.KEYCODE_FORWARD_DEL

            // Keypad enter
            96 -> KeyEvent.KEYCODE_NUMPAD_ENTER

            // Right side modifiers
            97 -> KeyEvent.KEYCODE_CTRL_RIGHT
            98 -> KeyEvent.KEYCODE_NUMPAD_DIVIDE
            99 -> KeyEvent.KEYCODE_SYSRQ

            // Additional keys
            100 -> KeyEvent.KEYCODE_ALT_RIGHT

            else -> {
                if (evdev in 1..255) evdev else 0
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun release() {
        // 停止所有重复
        synchronized(repeatingKeys) {
            repeatingKeys.clear()
            pendingRepeatKeys.clear()
            isRepeating = false
        }
        choreographer.removeFrameCallback(frameCallback)
        inputEventSender = null
    }
}
