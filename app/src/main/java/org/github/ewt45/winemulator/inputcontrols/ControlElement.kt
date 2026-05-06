package org.github.ewt45.winemulator.inputcontrols

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Range
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Shape
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Type
import java.util.Timer
import java.util.TimerTask
import kotlin.math.*

/**
 * ControlElement - 每个虚拟控件元素（按钮、摇杆等）的定义和逻辑
 * 
 * 完整移植自 termux-app 的 ExtraKeys 实现，整合了以下功能：
 * - 多种控件类型：普通按钮、D-Pad、摇杆、触摸板、范围按钮、组合按钮
 * - 触摸处理：支持按下、拖动、释放、长按重复
 * - 键码映射：支持 termux 的标准键码映射表
 * - 显示字符：支持多种显示风格（箭头、符号等）
 * - 按键别名：支持多种按键别名映射
 * - 状态管理：支持切换开关、持续按下等状态
 */
class ControlElement(
    private val inputControlsView: InputControlsView
) {
    companion object {
        // 摇杆和方向键的死区设置（来自 termux-app 和 Linbox 原有实现）
        const val STICK_DEAD_ZONE = 0.15f
        const val DPAD_DEAD_ZONE = 0.3f
        const val STICK_SENSITIVITY = 3.0f
        
        // 触摸板速度设置
        const val TRACKPAD_MIN_SPEED = 0.8f
        const val TRACKPAD_MAX_SPEED = 20.0f
        const val TRACKPAD_ACCELERATION_THRESHOLD: Byte = 4
        
        // 按钮最小保持时间（用于切换开关判定）
        const val BUTTON_MIN_TIME_TO_KEEP_PRESSED: Short = 300
        
        // 长按重复延迟（毫秒）
        const val LONG_PRESS_TIMEOUT = 400L
        const val LONG_PRESS_REPEAT_DELAY = 80L
        
        // Termux 风格的可重复按键列表
        val PRIMARY_REPETITIVE_KEYS = listOf(
            "UP", "DOWN", "LEFT", "RIGHT",
            "BKSP", "DEL",
            "PGUP", "PGDN"
        )
        
        /**
         * Termux 风格的键码映射表
         * 将字符串键名映射到 Android KeyEvent 键码
         */
        val PRIMARY_KEY_CODES_FOR_STRINGS = mapOf(
            "SPACE" to KeyEvent.KEYCODE_SPACE,
            "ESC" to KeyEvent.KEYCODE_ESCAPE,
            "TAB" to KeyEvent.KEYCODE_TAB,
            "HOME" to KeyEvent.KEYCODE_MOVE_HOME,
            "END" to KeyEvent.KEYCODE_MOVE_END,
            "PGUP" to KeyEvent.KEYCODE_PAGE_UP,
            "PGDN" to KeyEvent.KEYCODE_PAGE_DOWN,
            "INS" to KeyEvent.KEYCODE_INSERT,
            "DEL" to KeyEvent.KEYCODE_FORWARD_DEL,
            "BKSP" to KeyEvent.KEYCODE_DEL,
            "UP" to KeyEvent.KEYCODE_DPAD_UP,
            "LEFT" to KeyEvent.KEYCODE_DPAD_LEFT,
            "RIGHT" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "DOWN" to KeyEvent.KEYCODE_DPAD_DOWN,
            "ENTER" to KeyEvent.KEYCODE_ENTER,
            "F1" to KeyEvent.KEYCODE_F1,
            "F2" to KeyEvent.KEYCODE_F2,
            "F3" to KeyEvent.KEYCODE_F3,
            "F4" to KeyEvent.KEYCODE_F4,
            "F5" to KeyEvent.KEYCODE_F5,
            "F6" to KeyEvent.KEYCODE_F6,
            "F7" to KeyEvent.KEYCODE_F7,
            "F8" to KeyEvent.KEYCODE_F8,
            "F9" to KeyEvent.KEYCODE_F9,
            "F10" to KeyEvent.KEYCODE_F10,
            "F11" to KeyEvent.KEYCODE_F11,
            "F12" to KeyEvent.KEYCODE_F12,
            // Numpad 键码（扩展支持）
            "NUMPAD_0" to KeyEvent.KEYCODE_NUMPAD_0,
            "NUMPAD_1" to KeyEvent.KEYCODE_NUMPAD_1,
            "NUMPAD_2" to KeyEvent.KEYCODE_NUMPAD_2,
            "NUMPAD_3" to KeyEvent.KEYCODE_NUMPAD_3,
            "NUMPAD_4" to KeyEvent.KEYCODE_NUMPAD_4,
            "NUMPAD_5" to KeyEvent.KEYCODE_NUMPAD_5,
            "NUMPAD_6" to KeyEvent.KEYCODE_NUMPAD_6,
            "NUMPAD_7" to KeyEvent.KEYCODE_NUMPAD_7,
            "NUMPAD_8" to KeyEvent.KEYCODE_NUMPAD_8,
            "NUMPAD_9" to KeyEvent.KEYCODE_NUMPAD_9,
            "NUMPAD_ADD" to KeyEvent.KEYCODE_NUMPAD_ADD,
            "NUMPAD_SUBTRACT" to KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
            "NUMPAD_MULTIPLY" to KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
            "NUMPAD_DIVIDE" to KeyEvent.KEYCODE_NUMPAD_DIVIDE,
            "NUMPAD_DOT" to KeyEvent.KEYCODE_NUMPAD_DOT,
            "NUMPAD_ENTER" to KeyEvent.KEYCODE_NUMPAD_ENTER,
            "NUMPAD_EQUALS" to KeyEvent.KEYCODE_NUMPAD_EQUALS,
            "NUMPAD_LEFT_PAREN" to KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN,
            "NUMPAD_RIGHT_PAREN" to KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN,
            // 修饰键
            "CTRL" to KeyEvent.KEYCODE_CTRL_LEFT,
            "ALT" to KeyEvent.KEYCODE_ALT_LEFT,
            "SHIFT" to KeyEvent.KEYCODE_SHIFT_LEFT,
            "META" to KeyEvent.KEYCODE_META_LEFT,
            "FN" to KeyEvent.KEYCODE_FUNCTION
        )
        
        /**
         * Termux 风格的显示字符映射
         * 用于将键名显示为美观的符号
         */
        object EXTRA_KEY_DISPLAY_MAPS {
            // 经典箭头符号
            val CLASSIC_ARROWS_DISPLAY = mapOf(
                "LEFT" to "←",
                "RIGHT" to "→",
                "UP" to "↑",
                "DOWN" to "↓"
            )
            
            // 常用字符
            val WELL_KNOWN_CHARACTERS_DISPLAY = mapOf(
                "ENTER" to "↲",
                "TAB" to "↹",
                "BKSP" to "⌫",
                "DEL" to "⌦",
                "DRAWER" to "☰",
                "KEYBOARD" to "⌨",
                "PASTE" to "⎘",
                "SCROLL" to "⇳"
            )
            
            // 较少人知道的符号
            val LESS_KNOWN_CHARACTERS_DISPLAY = mapOf(
                "HOME" to "⇱",
                "END" to "⇲",
                "PGUP" to "⇑",
                "PGDN" to "⇓"
            )
            
            // 三角箭头（替代箭头）
            val ARROW_TRIANGLE_VARIATION_DISPLAY = mapOf(
                "LEFT" to "◀",
                "RIGHT" to "▶",
                "UP" to "▲",
                "DOWN" to "▼"
            )
            
            // ISO 符号（Ctrl、Alt、Esc）
            val NOT_KNOWN_ISO_CHARACTERS = mapOf(
                "CTRL" to "⎈",
                "ALT" to "⎇",
                "ESC" to "⎋"
            )
            
            // 更好看的显示
            val NICER_LOOKING_DISPLAY = mapOf(
                "-" to "―"
            )
            
            // 完整 ISO 字符映射（所有映射的组合）
            val FULL_ISO_CHAR_DISPLAY: Map<String, String> by lazy {
                CLASSIC_ARROWS_DISPLAY + WELL_KNOWN_CHARACTERS_DISPLAY + 
                LESS_KNOWN_CHARACTERS_DISPLAY + NICER_LOOKING_DISPLAY + 
                NOT_KNOWN_ISO_CHARACTERS
            }
            
            // 仅箭头
            val ARROWS_ONLY_CHAR_DISPLAY: Map<String, String> by lazy {
                CLASSIC_ARROWS_DISPLAY + NICER_LOOKING_DISPLAY
            }
            
            // 大量箭头
            val LOTS_OF_ARROWS_CHAR_DISPLAY: Map<String, String> by lazy {
                CLASSIC_ARROWS_DISPLAY + WELL_KNOWN_CHARACTERS_DISPLAY + 
                LESS_KNOWN_CHARACTERS_DISPLAY + NICER_LOOKING_DISPLAY
            }
            
            // 默认字符显示
            val DEFAULT_CHAR_DISPLAY: Map<String, String> by lazy {
                CLASSIC_ARROWS_DISPLAY + WELL_KNOWN_CHARACTERS_DISPLAY + 
                NICER_LOOKING_DISPLAY
            }
        }
        
        /**
         * Termux 风格的按键别名映射
         */
        val CONTROL_CHARS_ALIASES = mapOf(
            "ESCAPE" to "ESC",
            "CONTROL" to "CTRL",
            "SHFT" to "SHIFT",
            "RETURN" to "ENTER",
            "FUNCTION" to "FN",
            // 方向键缩写
            "LT" to "LEFT",
            "RT" to "RIGHT",
            "DN" to "DOWN",
            // Page Up/Down 变体
            "PAGEUP" to "PGUP",
            "PAGE_UP" to "PGUP",
            "PAGE UP" to "PGUP",
            "PAGE-UP" to "PGUP",
            "PAGEDOWN" to "PGDN",
            "PAGE_DOWN" to "PGDN",
            "PAGE-DOWN" to "PGDN",
            // Delete 变体
            "DELETE" to "DEL",
            "BACKSPACE" to "BKSP",
            // 特殊字符
            "BACKSLASH" to "\\",
            "QUOTE" to "\"",
            "APOSTROPHE" to "'"
        )
        
        /**
         * 获取指定风格的显示字符映射
         */
        fun getCharDisplayMapForStyle(style: String?): Map<String, String> {
            return when (style) {
                "arrows-only" -> EXTRA_KEY_DISPLAY_MAPS.ARROWS_ONLY_CHAR_DISPLAY
                "arrows-all" -> EXTRA_KEY_DISPLAY_MAPS.LOTS_OF_ARROWS_CHAR_DISPLAY
                "all" -> EXTRA_KEY_DISPLAY_MAPS.FULL_ISO_CHAR_DISPLAY
                "none" -> emptyMap()
                else -> EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY
            }
        }
        
        /**
         * 解析键名字符串，应用别名后返回规范化的键名
         */
        fun normalizeKeyName(keyName: String): String {
            var normalized = keyName.uppercase().trim()
            // 应用别名映射
            while (CONTROL_CHARS_ALIASES.containsKey(normalized)) {
                val alias = CONTROL_CHARS_ALIASES[normalized]
                if (alias != null) {
                    normalized = alias
                } else {
                    break
                }
            }
            return normalized
        }
        
        /**
         * 获取键名对应的显示字符
         */
        fun getDisplayForKey(keyName: String, displayMap: Map<String, String>? = null): String {
            val normalized = normalizeKeyName(keyName)
            val map = displayMap ?: EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY
            return map[normalized] ?: keyName
        }
        
        /**
         * 获取键名对应的 Android 键码
         */
        fun getKeyCodeForString(keyName: String): Int? {
            val normalized = normalizeKeyName(keyName)
            return PRIMARY_KEY_CODES_FOR_STRINGS[normalized]
        }
        
        /**
         * 检查是否为可重复按键
         */
        fun isRepetitiveKey(keyName: String): Boolean {
            val normalized = normalizeKeyName(keyName)
            return PRIMARY_REPETITIVE_KEYS.contains(normalized)
        }
    }
    
    /**
     * 控件类型枚举
     */
    enum class Type {
        BUTTON,          // 普通按钮
        D_PAD,           // 方向键
        RANGE_BUTTON,    // 范围按钮（如 A-Z, F1-F12）
        STICK,           // 摇杆
        TRACKPAD,        // 触摸板
        COMBINE_BUTTON,  // 组合按钮（同时触发多个按键）
        CHEAT_CODE_TEXT, // 作弊码文本
        EXTRA_KEY;       // Termux 风格的额外按键
        
        companion object {
            fun names(): Array<String> = entries.map { it.name.replace("_", "-") }.toTypedArray()
        }
    }
    
    /**
     * 控件形状枚举
     */
    enum class Shape {
        CIRCLE,      // 圆形
        RECT,        // 矩形
        ROUND_RECT,  // 圆角矩形
        SQUARE;      // 正方形
        
        companion object {
            fun names(): Array<String> = entries.map { it.name.replace("_", " ") }.toTypedArray()
        }
    }
    
    /**
     * 范围按钮的范围类型
     */
    enum class Range(val max: Byte) {
        FROM_A_TO_Z(26),
        FROM_0_TO_9(10),
        FROM_F1_TO_F12(12),
        FROM_NP0_TO_NP9(10);
        
        companion object {
            fun names(): Array<String> = entries.map { it.name.replace("_", " ") }.toTypedArray()
            
            fun fromString(name: String): Range? {
                return when (name) {
                    "FROM_A_TO_Z", "A-Z", "FROM-A-TO-Z" -> FROM_A_TO_Z
                    "FROM_0_TO_9", "0-9", "DIGITS", "FROM-0-TO-9" -> FROM_0_TO_9
                    "FROM_F1_TO_F12", "F1-F12", "FUNCTION_KEYS", "FROM-F1-TO-F12" -> FROM_F1_TO_F12
                    "FROM_NP0_TO_NP9", "NP0-NP9", "NUMPAD_DIGITS", "FROM-NP0-TO-NP9" -> FROM_NP0_TO_NP9
                    else -> null
                }
            }
        }
    }
    
    // ==================== 属性定义 ====================
    
    var type: Type = Type.BUTTON
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    
    var shape: Shape = Shape.CIRCLE
        set(value) {
            if (field != value) {
                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    
    // 绑定到输入动作
    private var bindings: Array<Binding> = arrayOf(Binding.NONE, Binding.NONE, Binding.NONE, Binding.NONE)
    
    // Termux 风格额外按键属性
    var extraKeyName: String? = null  // 如 "CTRL", "ALT", "LEFT"
    var extraKeyDisplay: String? = null  // 显示字符，如 "⎈", "←"
    var extraKeyMacro: String? = null  // 宏命令
    var extraKeyPopup: TermuxX11ExtraKeyButton? = null  // 弹出菜单
    
    var scale: Float = 1.0f
        set(value) {
            if (field != value) {
                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    
    var x: Int = 0
        set(value) {
            if (field != value) {
                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    
    var y: Int = 0
        set(value) {
            if (field != value) {
                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    
    var isSelected: Boolean = false
    var isToggleSwitch: Boolean = false
    var text: String = ""
        set(value) {
            field = value
        }
    
    var iconId: Byte = 0
        set(value) {
            field = value
        }
    
    var range: Range? = null
        set(value) {
            field = value
        }
    
    var orientation: Byte = 0
        set(value) {
            if (field != value) {
                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    
    var customIconId: String? = null
        set(value) {
            if (field != value) {
                field = value
                oldCustomIconId = value
                clipIcon = null
            }
        }
    private var oldCustomIconId: String? = null
    private var clipIcon: Bitmap? = null
    
    var backgroundColor: Int = 0
        set(value) {
            if (field != value) {
                field = value
                oldBackgroundColor = value
                clipIcon = null
            }
        }
    private var oldBackgroundColor: Int = -1
    
    var cheatCodeText: String = "None"
    private var cheatCodePressed = false
    
    // 显示风格（用于 Termux 风格按键）
    var displayStyle: String? = null
    
    // ==================== 状态管理 ====================
    
    private var currentPointerId: Int = -1
    private var isButtonHeldDown: Boolean = false
    private var heldDownBinding: Binding? = null
    private val boundingBox: Rect = Rect()
    private var boundingBoxNeedsUpdate: Boolean = true
    private val states: BooleanArray = booleanArrayOf(false, false, false, false)
    private var currentPosition: PointF? = null
    private var touchTime: Long? = null
    
    // 范围滚动器
    private var scroller: RangeScroller? = null
    private var interpolator: CubicBezierInterpolator? = null
    
    // 长按重复相关
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private val keyRepeatDelayMs = 50L
    private val keyRepeatIntervalMs = 16L
    
    // 持续按下 Timer（像 D_PAD 一样持续发送）
    private var holdTimer: Timer? = null
    private var holdTimerTask: TimerTask? = null
    private val holdIntervalMs = 16L
    
    // Termux 风格的长按计数器
    private var longPressCount = 0
    
    // ==================== 初始化方法 ====================
    
    private fun reset() {
        text = ""
        iconId = 0
        range = null
        scroller = null
        customIconId = null
        clipIcon = null
        backgroundColor = 0
        oldBackgroundColor = -1
        extraKeyName = null
        extraKeyDisplay = null
        extraKeyMacro = null
        extraKeyPopup = null
        stopKeyRepeat()
        boundingBoxNeedsUpdate = true
    }
    
    /**
     * 初始化默认绑定（根据控件类型）
     */
    fun initDefaultBindings() {
        when (type) {
            Type.D_PAD, Type.STICK, Type.COMBINE_BUTTON -> {
                bindings = arrayOf(Binding.KEY_W, Binding.KEY_D, Binding.KEY_S, Binding.KEY_A)
            }
            Type.TRACKPAD -> {
                bindings = arrayOf(
                    Binding.MOUSE_MOVE_UP,
                    Binding.MOUSE_MOVE_RIGHT,
                    Binding.MOUSE_MOVE_DOWN,
                    Binding.MOUSE_MOVE_LEFT
                )
            }
            Type.RANGE_BUTTON -> {
                scroller = RangeScroller(inputControlsView, this)
            }
            Type.EXTRA_KEY -> {
                // Extra Key 类型使用 extraKeyName 来确定键码
                initExtraKeyBindings()
            }
            else -> {}
        }
    }
    
    /**
     * 初始化 Termux 风格额外按键的绑定
     */
    private fun initExtraKeyBindings() {
        val keyName = extraKeyName ?: return
        
        // 尝试从键码映射获取绑定
        val keyCode = getKeyCodeForString(keyName)
        if (keyCode != null) {
            // 将键码转换为对应的 Binding
            bindings[0] = getBindingForKeyCode(keyCode)
        }
        
        // 如果键名是修饰键，设置特殊的显示
        if (extraKeyDisplay == null) {
            extraKeyDisplay = getDisplayForKey(keyName)
        }
    }
    
    /**
     * 根据 Android 键码获取对应的 Binding
     */
    private fun getBindingForKeyCode(keyCode: Int): Binding {
        return when (keyCode) {
            KeyEvent.KEYCODE_SPACE -> Binding.KEY_SPACE
            KeyEvent.KEYCODE_ESCAPE -> Binding.KEY_ESC
            KeyEvent.KEYCODE_TAB -> Binding.KEY_TAB
            KeyEvent.KEYCODE_ENTER -> Binding.KEY_ENTER
            KeyEvent.KEYCODE_DEL -> Binding.KEY_BACKSPACE
            KeyEvent.KEYCODE_FORWARD_DEL -> Binding.KEY_DELETE
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_MOVE_HOME -> Binding.KEY_UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MOVE_END -> Binding.KEY_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> Binding.KEY_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Binding.KEY_RIGHT
            KeyEvent.KEYCODE_PAGE_UP -> Binding.KEY_PAGE_UP
            KeyEvent.KEYCODE_PAGE_DOWN -> Binding.KEY_PAGE_DOWN
            KeyEvent.KEYCODE_INSERT -> Binding.KEY_INSERT
            KeyEvent.KEYCODE_F1 -> Binding.KEY_F1
            KeyEvent.KEYCODE_F2 -> Binding.KEY_F2
            KeyEvent.KEYCODE_F3 -> Binding.KEY_F3
            KeyEvent.KEYCODE_F4 -> Binding.KEY_F4
            KeyEvent.KEYCODE_F5 -> Binding.KEY_F5
            KeyEvent.KEYCODE_F6 -> Binding.KEY_F6
            KeyEvent.KEYCODE_F7 -> Binding.KEY_F7
            KeyEvent.KEYCODE_F8 -> Binding.KEY_F8
            KeyEvent.KEYCODE_F9 -> Binding.KEY_F9
            KeyEvent.KEYCODE_F10 -> Binding.KEY_F10
            KeyEvent.KEYCODE_F11 -> Binding.KEY_F11
            KeyEvent.KEYCODE_F12 -> Binding.KEY_F12
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> Binding.KEY_LEFT_CTRL
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> Binding.KEY_LEFT_ALT
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> Binding.KEY_LEFT_SHIFT
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> Binding.KEY_LEFT_META
            else -> Binding.NONE
        }
    }
    
    // ==================== 绑定管理 ====================
    
    fun getBindingCount(): Int = bindings.size
    
    fun setBindingCount(count: Int) {
        bindings = Array(count) { Binding.NONE }
        states.fill(false)
        boundingBoxNeedsUpdate = true
    }
    
    fun getBindingAt(index: Int): Binding = if (index < bindings.size) bindings[index] else Binding.NONE
    
    @Suppress("UNCHECKED_CAST")
    fun setBindingAt(index: Int, binding: Binding) {
        if (index >= bindings.size) {
            val oldLength = bindings.size
            val newBindings = arrayOfNulls<Binding>(index + 1) as Array<Binding>
            for (i in bindings.indices) {
                newBindings[i] = bindings[i]
            }
            for (i in oldLength until newBindings.size) {
                newBindings[i] = Binding.NONE
            }
            bindings = newBindings
            states.fill(false)
            boundingBoxNeedsUpdate = true
        }
        bindings[index] = binding
    }
    
    fun setBinding(binding: Binding) {
        bindings.fill(binding)
    }
    
    // ==================== 边界框计算 ====================
    
    fun getBoundingBox(): Rect {
        if (boundingBoxNeedsUpdate) computeBoundingBox()
        return boundingBox
    }
    
    private fun computeBoundingBox() {
        val snappingSize = inputControlsView.snappingSize
        var halfWidth = 0
        var halfHeight = 0
        
        when (type) {
            Type.BUTTON, Type.COMBINE_BUTTON, Type.CHEAT_CODE_TEXT -> {
                when (shape) {
                    Shape.RECT, Shape.ROUND_RECT -> {
                        halfWidth = snappingSize * 4
                        halfHeight = snappingSize * 2
                    }
                    Shape.SQUARE -> {
                        halfWidth = (snappingSize * 2.5f).toInt()
                        halfHeight = (snappingSize * 2.5f).toInt()
                    }
                    Shape.CIRCLE -> {
                        halfWidth = snappingSize * 3
                        halfHeight = snappingSize * 3
                    }
                }
            }
            Type.D_PAD -> {
                halfWidth = snappingSize * 7
                halfHeight = snappingSize * 7
            }
            Type.TRACKPAD, Type.STICK -> {
                halfWidth = snappingSize * 6
                halfHeight = snappingSize * 6
            }
            Type.EXTRA_KEY -> {
                // Termux 风格按键使用固定大小
                halfWidth = (snappingSize * 3).toInt()
                halfHeight = (snappingSize * 2).toInt()
            }
            Type.RANGE_BUTTON -> {
                halfWidth = (bindings.size * 4 * snappingSize) / 2
                halfHeight = snappingSize * 2
                
                if (orientation == 1.toByte()) {
                    val tmp = halfWidth
                    halfWidth = halfHeight
                    halfHeight = tmp
                }
            }
        }
        
        halfWidth = (halfWidth * scale).toInt()
        halfHeight = (halfHeight * scale).toInt()
        boundingBox.set(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight)
        boundingBoxNeedsUpdate = false
    }
    
    fun containsPoint(px: Float, py: Float): Boolean {
        return getBoundingBox().contains((px + 0.5f).toInt(), (py + 0.5f).toInt())
    }
    
    // ==================== 绘制方法 ====================
    
    fun draw(canvas: Canvas) {
        val snappingSize = inputControlsView.snappingSize
        val paint = inputControlsView.getPaint()
        val primaryColor = inputControlsView.getPrimaryColor()
        
        paint.color = if (isSelected) inputControlsView.getSecondaryColor() else primaryColor
        paint.style = Paint.Style.STROKE
        val strokeWidth = snappingSize * 0.25f
        paint.strokeWidth = strokeWidth
        val box = getBoundingBox()
        
        when (type) {
            Type.BUTTON, Type.COMBINE_BUTTON, Type.CHEAT_CODE_TEXT -> drawButton(canvas, paint, box, primaryColor, strokeWidth)
            Type.D_PAD -> drawDPad(canvas, paint, box)
            Type.RANGE_BUTTON -> drawRangeButton(canvas, paint, box, strokeWidth)
            Type.STICK -> drawStick(canvas, paint, box, primaryColor, strokeWidth)
            Type.TRACKPAD -> drawTrackpad(canvas, paint, box, strokeWidth)
            Type.EXTRA_KEY -> drawExtraKey(canvas, paint, box, primaryColor, strokeWidth)
        }
    }
    
    /**
     * 绘制 Termux 风格的额外按键
     */
    private fun drawExtraKey(canvas: Canvas, paint: Paint, box: Rect, primaryColor: Int, strokeWidth: Float) {
        val cx = box.centerX().toFloat()
        val cy = box.centerY().toFloat()
        
        // 绘制背景
        when (shape) {
            Shape.CIRCLE -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(30, 255, 255, 255)
                canvas.drawCircle(cx, cy, box.width() * 0.5f, paint)
                paint.style = Paint.Style.STROKE
                paint.color = if (isSelected) inputControlsView.getSecondaryColor() else primaryColor
                canvas.drawCircle(cx, cy, box.width() * 0.5f, paint)
            }
            Shape.ROUND_RECT -> {
                val radius = box.height() * 0.3f
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(30, 255, 255, 255)
                canvas.drawRoundRect(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat(),
                    radius, radius, paint
                )
                paint.style = Paint.Style.STROKE
                paint.color = if (isSelected) inputControlsView.getSecondaryColor() else primaryColor
                canvas.drawRoundRect(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat(),
                    radius, radius, paint
                )
            }
            else -> {
                // 默认使用圆角矩形
                val radius = box.height() * 0.3f
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(30, 255, 255, 255)
                canvas.drawRoundRect(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat(),
                    radius, radius, paint
                )
                paint.style = Paint.Style.STROKE
                paint.color = if (isSelected) inputControlsView.getSecondaryColor() else primaryColor
                canvas.drawRoundRect(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat(),
                    radius, radius, paint
                )
            }
        }
        
        // 绘制显示文本
        val displayText = extraKeyDisplay ?: text.ifEmpty { getDisplayText() }
        paint.textSize = minOf(
            getTextSizeForWidth(paint, displayText, box.width() - strokeWidth * 2),
            inputControlsView.snappingSize * 2 * scale
        )
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        canvas.drawText(
            displayText, x.toFloat(),
            y - (paint.descent() + paint.ascent()) * 0.5f,
            paint
        )
    }
    
    private fun drawButton(canvas: Canvas, paint: Paint, box: Rect, primaryColor: Int, strokeWidth: Float) {
        val cx = box.centerX().toFloat()
        val cy = box.centerY().toFloat()
        
        when (shape) {
            Shape.CIRCLE -> {
                canvas.drawCircle(cx, cy, box.width() * 0.5f, paint)
            }
            Shape.RECT -> {
                canvas.drawRect(box, paint)
            }
            Shape.ROUND_RECT -> {
                val radius = box.height() * 0.5f
                canvas.drawRoundRect(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat(),
                    radius, radius, paint
                )
            }
            Shape.SQUARE -> {
                val snappingSize = inputControlsView.snappingSize
                val radius = snappingSize * 0.75f * scale
                canvas.drawRoundRect(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat(),
                    radius, radius, paint
                )
            }
        }
        
        if (!customIconId.isNullOrEmpty()) {
            drawCustomIcon(canvas, cx, cy, box.width().toFloat(), box.height().toFloat())
        } else if (backgroundColor > 0) {
            drawColorSolidIcon(canvas, cx, cy, box.width().toFloat(), box.height().toFloat())
        } else if (iconId > 0) {
            drawIcon(canvas, cx, cy, box.width().toFloat(), box.height().toFloat())
        } else {
            val displayText = getDisplayText()
            paint.textSize = minOf(
                getTextSizeForWidth(paint, displayText, box.width() - strokeWidth * 2),
                inputControlsView.snappingSize * 2 * scale
            )
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL
            paint.color = primaryColor
            canvas.drawText(
                displayText, x.toFloat(),
                y - (paint.descent() + paint.ascent()) * 0.5f,
                paint
            )
        }
    }
    
    private fun drawIcon(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
        val paint = inputControlsView.getPaint()
        val icon = inputControlsView.getIcon(iconId) ?: return
        
        paint.colorFilter = inputControlsView.getColorFilter()
        val margin = (inputControlsView.snappingSize * (if (shape == Shape.CIRCLE || shape == Shape.SQUARE) 2.0f else 1.0f) * scale).toInt()
        val halfSize = ((minOf(width, height) - margin) * 0.5f).toInt()
        
        val srcRect = Rect(0, 0, icon.width, icon.height)
        val dstRect = Rect(
            (cx - halfSize).toInt(),
            (cy - halfSize).toInt(),
            (cx + halfSize).toInt(),
            (cy + halfSize).toInt()
        )
        
        canvas.drawBitmap(icon, srcRect, dstRect, paint)
        paint.colorFilter = null
    }
    
    private fun drawCustomIcon(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
        val paint = inputControlsView.getPaint()
        val iconId = customIconId ?: return
        
        var icon: Bitmap? = if (clipIcon != null && oldCustomIconId == iconId) {
            clipIcon
        } else {
            val iconOrigin = inputControlsView.getCustomIcon(iconId) ?: return
            val isCycle = shape == Shape.CIRCLE
            val clipped = inputControlsView.clipBitmap(iconOrigin, isCycle) ?: return
            clipIcon = clipped
            oldCustomIconId = iconId
            inputControlsView.counterMapIncrease(iconId)
            clipped
        }
        
        if (icon == null) return
        
        val margin = (inputControlsView.snappingSize * (if (shape == Shape.CIRCLE || shape == Shape.SQUARE) 2.0f else 1.0f) * scale).toInt()
        val halfSize = ((minOf(width, height) - margin) * 0.7f).toInt()
        
        val srcRect = Rect(0, 0, icon.width, icon.height)
        val dstRect = Rect(
            (cx - halfSize).toInt(),
            (cy - halfSize).toInt(),
            (cx + halfSize).toInt(),
            (cy + halfSize).toInt()
        )
        
        canvas.drawBitmap(icon, srcRect, dstRect, paint)
        paint.colorFilter = null
    }
    
    private fun drawColorSolidIcon(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
        val paint = inputControlsView.getPaint()
        val color = backgroundColor
        
        var icon: Bitmap? = if (clipIcon != null && oldBackgroundColor == color) {
            clipIcon
        } else {
            val isCycle = shape == Shape.CIRCLE
            val created = inputControlsView.createShapeBitmap(width, height, toARGB(color), isCycle) ?: return
            clipIcon = created
            oldBackgroundColor = color
            created
        }
        
        if (icon == null) return
        
        val margin = (inputControlsView.snappingSize * (if (shape == Shape.CIRCLE || shape == Shape.SQUARE) 2.0f else 1.0f) * scale).toInt()
        val halfSize = ((minOf(width, height) - margin) * 0.7f).toInt()
        
        val srcRect = Rect(0, 0, icon.width, icon.height)
        val dstRect = Rect(
            (cx - halfSize).toInt(),
            (cy - halfSize).toInt(),
            (cx + halfSize).toInt(),
            (cy + halfSize).toInt()
        )
        
        canvas.drawBitmap(icon, srcRect, dstRect, paint)
        paint.colorFilter = null
    }
    
    private fun toARGB(rgb: Int): Int {
        return Color.argb(255, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
    }
    
    private fun drawDPad(canvas: Canvas, paint: Paint, box: Rect) {
        val cx = box.centerX().toFloat()
        val cy = box.centerY().toFloat()
        val snappingSize = inputControlsView.snappingSize
        val offsetX = snappingSize * 2 * scale
        val offsetY = snappingSize * 3 * scale
        val start = snappingSize * scale
        
        val path = inputControlsView.getPath()
        path.reset()
        
        // 上
        path.moveTo(cx, cy - start)
        path.lineTo(cx - offsetX, cy - offsetY)
        path.lineTo(cx - offsetX, box.top.toFloat())
        path.lineTo(cx + offsetX, box.top.toFloat())
        path.lineTo(cx + offsetX, cy - offsetY)
        path.close()
        
        // 左
        path.moveTo(cx - start, cy)
        path.lineTo(cx - offsetY, cy - offsetX)
        path.lineTo(box.left.toFloat(), cy - offsetX)
        path.lineTo(box.left.toFloat(), cy + offsetX)
        path.lineTo(cx - offsetY, cy + offsetX)
        path.close()
        
        // 下
        path.moveTo(cx, cy + start)
        path.lineTo(cx - offsetX, cy + offsetY)
        path.lineTo(cx - offsetX, box.bottom.toFloat())
        path.lineTo(cx + offsetX, box.bottom.toFloat())
        path.lineTo(cx + offsetX, cy + offsetY)
        path.close()
        
        // 右
        path.moveTo(cx + start, cy)
        path.lineTo(cx + offsetY, cy - offsetX)
        path.lineTo(box.right.toFloat(), cy - offsetX)
        path.lineTo(box.right.toFloat(), cy + offsetX)
        path.lineTo(cx + offsetY, cy + offsetX)
        path.close()
        
        canvas.drawPath(path, paint)
        
        // 中心指示器
        val indicatorSize = snappingSize * 0.75f * scale
        path.reset()
        path.moveTo(cx, cy - indicatorSize)
        path.lineTo(cx + indicatorSize, cy)
        path.lineTo(cx, cy + indicatorSize)
        path.lineTo(cx - indicatorSize, cy)
        path.close()
        canvas.drawPath(path, paint)
    }
    
    private fun drawRangeButton(canvas: Canvas, paint: Paint, box: Rect, strokeWidth: Float) {
        val snappingSize = inputControlsView.snappingSize
        val radius = snappingSize * 0.75f * scale
        
        if (orientation == 0.toByte()) {
            val lineTop = box.top + strokeWidth * 0.5f
            val lineBottom = box.bottom - strokeWidth * 0.5f
            
            canvas.drawRoundRect(
                box.left.toFloat(), box.top.toFloat(),
                box.right.toFloat(), box.bottom.toFloat(),
                radius, radius, paint
            )
            
            canvas.save()
            val clipPath = inputControlsView.getPath()
            clipPath.reset()
            clipPath.addRoundRect(
                box.left.toFloat(), box.top.toFloat(),
                box.right.toFloat(), box.bottom.toFloat(),
                radius, radius, Path.Direction.CW
            )
            canvas.clipPath(clipPath)
            
            val elementSize = scroller?.getElementSize() ?: run {
                val boxWidth = box.width().toFloat()
                val boxHeight = box.height().toFloat()
                maxOf(boxWidth, boxHeight) / getBindingCount()
            }
            val currentRange = range ?: Range.FROM_A_TO_Z
            val scrollOffset = scroller?.getScrollOffset() ?: 0f
            val rangeIndex = scroller?.getRangeIndex() ?: intArrayOf(0, currentRange.max.toInt())
            
            val initialOffset = scrollOffset % elementSize
            var startX = box.left.toFloat() - initialOffset
            
            for (i in rangeIndex[0] until rangeIndex[1]) {
                val index = i % currentRange.max.toInt()
                
                paint.style = Paint.Style.STROKE
                paint.color = paint.color
                
                if (startX > box.left && startX < box.right) {
                    canvas.drawLine(startX, lineTop, startX, lineBottom, paint)
                }
                
                val text = getRangeTextForIndex(currentRange, index)
                if (startX < box.right && startX + elementSize > box.left) {
                    paint.style = Paint.Style.FILL
                    paint.color = inputControlsView.getPrimaryColor()
                    paint.textSize = minOf(
                        getTextSizeForWidth(paint, text, elementSize - strokeWidth * 2),
                        snappingSize * 2 * scale
                    )
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(
                        text,
                        startX + elementSize * 0.5f,
                        y - (paint.descent() + paint.ascent()) * 0.5f,
                        paint
                    )
                }
                startX += elementSize
            }
            
            canvas.restore()
        } else {
            val lineLeft = box.left + strokeWidth * 0.5f
            val lineRight = box.right - strokeWidth * 0.5f
            
            canvas.drawRoundRect(
                box.left.toFloat(), box.top.toFloat(),
                box.right.toFloat(), box.bottom.toFloat(),
                radius, radius, paint
            )
            
            canvas.save()
            val clipPath = inputControlsView.getPath()
            clipPath.reset()
            clipPath.addRoundRect(
                box.left.toFloat(), box.top.toFloat(),
                box.right.toFloat(), box.bottom.toFloat(),
                radius, radius, Path.Direction.CW
            )
            canvas.clipPath(clipPath)
            
            val elementSize = scroller?.getElementSize() ?: run {
                val boxWidth = box.width().toFloat()
                val boxHeight = box.height().toFloat()
                maxOf(boxWidth, boxHeight) / getBindingCount()
            }
            val currentRange = range ?: Range.FROM_A_TO_Z
            val scrollOffset = scroller?.getScrollOffset() ?: 0f
            val rangeIndex = scroller?.getRangeIndex() ?: intArrayOf(0, currentRange.max.toInt())
            
            val initialOffset = scrollOffset % elementSize
            var startY = box.top.toFloat() - initialOffset
            
            for (i in rangeIndex[0] until rangeIndex[1]) {
                val index = i % currentRange.max.toInt()
                
                paint.style = Paint.Style.STROKE
                paint.color = paint.color
                
                if (startY > box.top && startY < box.bottom) {
                    canvas.drawLine(lineLeft, startY, lineRight, startY, paint)
                }
                
                val text = getRangeTextForIndex(currentRange, index)
                if (startY < box.bottom && startY + elementSize > box.top) {
                    paint.style = Paint.Style.FILL
                    paint.color = inputControlsView.getPrimaryColor()
                    paint.textSize = minOf(
                        getTextSizeForWidth(paint, text, box.width() - strokeWidth * 2),
                        snappingSize * 2 * scale
                    )
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(
                        text,
                        x.toFloat(),
                        startY + elementSize * 0.5f - (paint.descent() + paint.ascent()) * 0.5f,
                        paint
                    )
                }
                startY += elementSize
            }
            
            canvas.restore()
        }
    }
    
    private fun drawStick(canvas: Canvas, paint: Paint, box: Rect, primaryColor: Int, strokeWidth: Float) {
        val cx = box.centerX().toFloat()
        val cy = box.centerY().toFloat()
        val snappingSize = inputControlsView.snappingSize
        val oldColor = paint.color
        
        canvas.drawCircle(cx, cy, box.height() * 0.5f, paint)
        
        val thumbX = currentPosition?.x ?: cx
        val thumbY = currentPosition?.y ?: cy
        val thumbRadius = snappingSize * 3.5f * scale
        
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(50, 255, 255, 255)
        canvas.drawCircle(thumbX, thumbY, thumbRadius, paint)
        
        paint.style = Paint.Style.STROKE
        paint.color = oldColor
        canvas.drawCircle(thumbX, thumbY, thumbRadius + strokeWidth * 0.5f, paint)
    }
    
    private fun drawTrackpad(canvas: Canvas, paint: Paint, box: Rect, strokeWidth: Float) {
        val radius = box.height() * 0.15f
        canvas.drawRoundRect(
            box.left.toFloat(), box.top.toFloat(),
            box.right.toFloat(), box.bottom.toFloat(),
            radius, radius, paint
        )
        
        val offset = strokeWidth * 2.5f
        val innerStrokeWidth = strokeWidth * 2
        val innerHeight = box.height() - offset * 2
        val innerRadius = (innerHeight.toFloat() / box.height()) * radius - (innerStrokeWidth * 0.5f + strokeWidth * 0.5f)
        
        paint.strokeWidth = innerStrokeWidth
        canvas.drawRoundRect(
            box.left + offset, box.top + offset,
            box.right - offset, box.bottom - offset,
            innerRadius, innerRadius, paint
        )
        paint.strokeWidth = strokeWidth
    }
    
    private fun getDisplayText(): String {
        if (text.isNotEmpty()) {
            return text
        }
        
        // 对于 EXTRA_KEY 类型，使用 extraKeyDisplay
        if (type == Type.EXTRA_KEY && extraKeyDisplay != null) {
            return extraKeyDisplay!!
        }
        
        val binding = getBindingAt(0)
        var displayText = binding.toString()
            .replace("NUMPAD ", "NP")
            .replace("BUTTON ", "")
        
        if (displayText.length > 7) {
            val parts = displayText.split(" ")
            val sb = StringBuilder()
            for (part in parts) {
                if (part.isNotEmpty()) {
                    sb.append(part[0])
                }
            }
            displayText = (if (binding.isMouse) "M" else "") + sb
        }
        return displayText
    }
    
    private fun getTextSizeForWidth(paint: Paint, text: String, desiredWidth: Float): Float {
        val testTextSize = 48f
        paint.textSize = testTextSize
        return testTextSize * desiredWidth / paint.measureText(text)
    }
    
    private fun getRangeTextForIndex(range: Range, index: Int): String {
        return when (range) {
            Range.FROM_A_TO_Z -> ('A'.code + index).toChar().toString()
            Range.FROM_0_TO_9 -> ((index + 1) % 10).toString()
            Range.FROM_F1_TO_F12 -> "F${index + 1}"
            Range.FROM_NP0_TO_NP9 -> "NP${(index + 1) % 10}"
        }
    }
    
    // ==================== 触摸处理 ====================
    
    fun handleTouchDown(pointerId: Int, px: Float, py: Float): Boolean {
        if (currentPointerId == -1 && containsPoint(px, py)) {
            currentPointerId = pointerId
            longPressCount = 0
            
            when (type) {
                Type.CHEAT_CODE_TEXT -> {
                    if (!cheatCodePressed) {
                        for (c in cheatCodeText) {
                            inputControlsView.handleInputEvent(Binding.NONE, true)
                        }
                        cheatCodePressed = true
                    }
                    return true
                }
                Type.COMBINE_BUTTON -> {
                    if (isKeepButtonPressedAfterMinTime()) {
                        touchTime = System.currentTimeMillis()
                    }
                    if (!isToggleSwitch || !isSelected) {
                        for (i in states.indices) {
                            if (getBindingAt(i) != Binding.NONE) {
                                inputControlsView.handleInputEvent(getBindingAt(i), true)
                            }
                        }
                    }
                    return true
                }
                Type.BUTTON -> {
                    if (isKeepButtonPressedAfterMinTime()) {
                        touchTime = System.currentTimeMillis()
                    }
                    if (!isToggleSwitch || !isSelected) {
                        val binding = getBindingAt(0)
                        inputControlsView.handleInputEvent(binding, true)
                        isButtonHeldDown = true
                        startButtonHoldTimer(binding)
                        
                        // 如果是可重复按键，启动长按重复
                        if (extraKeyName != null && isRepetitiveKey(extraKeyName!!)) {
                            startKeyRepeat(binding)
                        }
                    }
                    return true
                }
                Type.EXTRA_KEY -> {
                    // Termux 风格额外按键的处理
                    return handleExtraKeyTouchDown(pointerId, px, py)
                }
                Type.RANGE_BUTTON -> {
                    if (scroller == null) {
                        scroller = RangeScroller(inputControlsView, this)
                    }
                    scroller?.handleTouchDown(px, py)
                    return true
                }
                Type.TRACKPAD -> {
                    if (currentPosition == null) currentPosition = PointF()
                    currentPosition?.set(px, py)
                    return handleTouchMove(pointerId, px, py)
                }
                Type.D_PAD, Type.STICK -> {
                    return handleTouchMove(pointerId, px, py)
                }
            }
        }
        return false
    }
    
    /**
     * 处理 Termux 风格额外按键的触摸按下
     */
    private fun handleExtraKeyTouchDown(pointerId: Int, px: Float, py: Float): Boolean {
        val keyName = extraKeyName ?: return false
        
        // 发送按键事件
        val keyCode = getKeyCodeForString(keyName)
        if (keyCode != null) {
            inputControlsView.handleInputEvent(getBindingAt(0), true)
        }
        
        // 如果是可重复按键，启动长按重复
        if (isRepetitiveKey(keyName)) {
            val binding = getBindingAt(0)
            startKeyRepeat(binding)
        }
        
        return true
    }
    
    private fun startKeyRepeat(binding: Binding) {
        stopKeyRepeat()
        repeatRunnable = object : Runnable {
            override fun run() {
                if (currentPointerId != -1) {
                    inputControlsView.handleInputEvent(binding, true)
                    longPressCount++
                    repeatHandler.postDelayed(this, LONG_PRESS_REPEAT_DELAY)
                }
            }
        }
        repeatHandler.postDelayed(repeatRunnable!!, LONG_PRESS_TIMEOUT)
    }
    
    private fun stopKeyRepeat() {
        repeatHandler.let {
            repeatRunnable?.let { task -> it.removeCallbacks(task) }
            repeatRunnable = null
        }
    }
    
    /**
     * 启动持续按键 Timer
     */
    private fun startButtonHoldTimer(binding: Binding) {
        stopButtonHoldTimer()
        heldDownBinding = binding
        
        holdTimer = Timer()
        holdTimerTask = object : TimerTask() {
            override fun run() {
                if (currentPointerId != -1 && heldDownBinding != null) {
                    inputControlsView.handleInputEvent(heldDownBinding!!, true)
                }
            }
        }
        holdTimer?.scheduleAtFixedRate(holdTimerTask, 0, holdIntervalMs)
    }
    
    /**
     * 停止持续按键 Timer
     */
    private fun stopButtonHoldTimer() {
        holdTimer?.cancel()
        holdTimer = null
        holdTimerTask = null
        heldDownBinding = null
    }
    
    fun handleTouchMove(pointerId: Int, px: Float, py: Float): Boolean {
        if (pointerId == currentPointerId) {
            when (type) {
                Type.BUTTON -> {
                    // BUTTON 类型不需要在 MOVE 时做任何事情
                    return true
                }
                Type.D_PAD, Type.STICK, Type.TRACKPAD -> {
                    var deltaX: Float
                    var deltaY: Float
                    val box = getBoundingBox()
                    val radius = box.width() * 0.5f
                    
                    when (type) {
                        Type.TRACKPAD -> {
                            val touchpadView = inputControlsView.touchpadView
                            if (currentPosition == null) currentPosition = PointF()
                            val deltaPoint = touchpadView?.computeDeltaPoint(currentPosition!!.x, currentPosition!!.y, px, py)
                                ?: floatArrayOf(0f, 0f)
                            deltaX = deltaPoint[0]
                            deltaY = deltaPoint[1]
                            currentPosition?.set(px, py)
                        }
                        else -> {
                            val localX = px - box.left
                            val localY = py - box.top
                            var offsetX = localX - radius
                            var offsetY = localY - radius
                            
                            val distance = sqrt((radius - localX) * (radius - localX) + (radius - localY) * (radius - localY))
                            if (distance > radius) {
                                val angle = atan2(offsetY, offsetX)
                                offsetX = (cos(angle) * radius).toFloat()
                                offsetY = (sin(angle) * radius).toFloat()
                            }
                            
                            deltaX = clamp(offsetX / radius, -1f, 1f)
                            deltaY = clamp(offsetY / radius, -1f, 1f)
                        }
                    }
                    
                    when (type) {
                        Type.STICK -> {
                            if (currentPosition == null) currentPosition = PointF()
                            currentPosition?.x = box.left + deltaX * radius + radius
                            currentPosition?.y = box.top + deltaY * radius + radius
                            
                            val newStates = booleanArrayOf(
                                deltaY <= -STICK_DEAD_ZONE,
                                deltaX >= STICK_DEAD_ZONE,
                                deltaY >= STICK_DEAD_ZONE,
                                deltaX <= -STICK_DEAD_ZONE
                            )
                            
                            for (i in 0..3) {
                                val value = if (i == 1 || i == 3) deltaX else deltaY
                                val binding = getBindingAt(i)
                                
                                if (binding.isGamepad) {
                                    val adjustedValue = clamp(
                                        maxOf(0f, abs(value) - 0.01f) * sign(value) * STICK_SENSITIVITY,
                                        -1f, 1f
                                    )
                                    inputControlsView.handleInputEvent(binding, true, adjustedValue)
                                    states[i] = true
                                } else {
                                    val state = if (binding.isMouseMove()) (newStates[i] || newStates[(i + 2) % 4]) else newStates[i]
                                    inputControlsView.handleInputEvent(binding, state, value)
                                    states[i] = state
                                }
                            }
                            inputControlsView.invalidate()
                        }
                        Type.TRACKPAD -> {
                            val newStates = booleanArrayOf(
                                deltaY <= -TRACKPAD_MIN_SPEED,
                                deltaX >= TRACKPAD_MIN_SPEED,
                                deltaY >= TRACKPAD_MIN_SPEED,
                                deltaX <= -TRACKPAD_MIN_SPEED
                            )
                            var cursorDx = 0
                            var cursorDy = 0
                            
                            for (i in 0..3) {
                                val value = if (i == 1 || i == 3) deltaX else deltaY
                                val binding = getBindingAt(i)
                                
                                if (binding.isGamepad) {
                                    if (abs(value) > TRACKPAD_ACCELERATION_THRESHOLD) {
                                        inputControlsView.handleInputEvent(binding, true, value * STICK_SENSITIVITY)
                                    }
                                    states[i] = true
                                } else {
                                    if (abs(value) > 4) {
                                        when (binding) {
                                            Binding.MOUSE_MOVE_LEFT, Binding.MOUSE_MOVE_RIGHT -> cursorDx = round(value).toInt()
                                            Binding.MOUSE_MOVE_UP, Binding.MOUSE_MOVE_DOWN -> cursorDy = round(value).toInt()
                                            else -> {
                                                inputControlsView.handleInputEvent(binding, newStates[i], value)
                                                states[i] = newStates[i]
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (cursorDx != 0 || cursorDy != 0) {
                                inputControlsView.injectPointerMove(cursorDx, cursorDy)
                            }
                        }
                        Type.D_PAD -> {
                            val newStates = booleanArrayOf(
                                deltaY <= -DPAD_DEAD_ZONE,
                                deltaX >= DPAD_DEAD_ZONE,
                                deltaY >= DPAD_DEAD_ZONE,
                                deltaX <= -DPAD_DEAD_ZONE
                            )
                            
                            for (i in 0..3) {
                                val value = if (i == 1 || i == 3) deltaX else deltaY
                                val binding = getBindingAt(i)
                                val state = if (binding.isMouseMove()) (newStates[i] || newStates[(i + 2) % 4]) else newStates[i]
                                
                                if (state) {
                                    inputControlsView.handleInputEvent(binding, true, value)
                                } else if (states[i]) {
                                    inputControlsView.handleInputEvent(binding, false, value)
                                }
                                
                                states[i] = state
                            }
                        }
                        else -> {}
                    }
                    return true
                }
                Type.RANGE_BUTTON -> {
                    scroller?.handleTouchMove(px, py)
                    inputControlsView.invalidate()
                    return true
                }
                else -> {
                    return false
                }
            }
        }
        return false
    }
    
    fun ownsPointer(pointerId: Int): Boolean = currentPointerId == pointerId
    
    fun handleTouchUp(pointerId: Int): Boolean {
        if (pointerId == currentPointerId) {
            when (type) {
                Type.CHEAT_CODE_TEXT -> {
                    cheatCodePressed = false
                }
                Type.COMBINE_BUTTON -> {
                    stopKeyRepeat()
                    if (isKeepButtonPressedAfterMinTime() && touchTime != null) {
                        isSelected = (System.currentTimeMillis() - touchTime!!) > BUTTON_MIN_TIME_TO_KEEP_PRESSED
                        if (!isSelected) {
                            for (i in states.indices.reversed()) {
                                if (getBindingAt(i) != Binding.NONE) {
                                    inputControlsView.handleInputEvent(getBindingAt(i), false)
                                }
                            }
                        }
                        touchTime = null
                        inputControlsView.invalidate()
                    } else if (!isToggleSwitch || isSelected) {
                        for (i in states.indices.reversed()) {
                            if (getBindingAt(i) != Binding.NONE) {
                                inputControlsView.handleInputEvent(getBindingAt(i), false)
                            }
                        }
                    }
                    
                    if (isToggleSwitch) {
                        isSelected = !isSelected
                        inputControlsView.invalidate()
                    }
                }
                Type.BUTTON -> {
                    val binding = getBindingAt(0)
                    if (isButtonHeldDown) {
                        inputControlsView.handleInputEvent(binding, false)
                        isButtonHeldDown = false
                    }
                    stopButtonHoldTimer()
                    stopKeyRepeat()
                    
                    if (isToggleSwitch) {
                        isSelected = !isSelected
                        inputControlsView.invalidate()
                    }
                }
                Type.EXTRA_KEY -> {
                    // Termux 风格额外按键的释放处理
                    handleExtraKeyTouchUp()
                }
                Type.RANGE_BUTTON, Type.D_PAD, Type.STICK, Type.TRACKPAD -> {
                    stopKeyRepeat()
                    for (i in states.indices) {
                        if (states[i]) inputControlsView.handleInputEvent(getBindingAt(i), false)
                        states[i] = false
                    }
                    
                    if (type == Type.RANGE_BUTTON) {
                        scroller?.handleTouchUp()
                    } else if (type == Type.STICK) {
                        inputControlsView.invalidate()
                    }
                    
                    currentPosition = null
                }
            }
            currentPointerId = -1
            return true
        }
        return false
    }
    
    /**
     * 处理 Termux 风格额外按键的触摸释放
     */
    private fun handleExtraKeyTouchUp() {
        val keyName = extraKeyName ?: return
        
        // 发送按键释放事件
        val keyCode = getKeyCodeForString(keyName)
        if (keyCode != null) {
            inputControlsView.handleInputEvent(getBindingAt(0), false)
        }
        
        // 如果是长按触发的重复（longPressCount > 0），不要再次发送释放
        // 因为重复处理已经包含了释放逻辑
        if (longPressCount > 0) {
            // 已经在 startKeyRepeat 的定时器中处理了释放
            return
        }
        
        // 停止重复
        stopKeyRepeat()
    }
    
    private fun isKeepButtonPressedAfterMinTime(): Boolean {
        val binding = getBindingAt(0)
        return !isToggleSwitch && (binding == Binding.GAMEPAD_BUTTON_THUMBL || binding == Binding.GAMEPAD_BUTTON_THUMBR)
    }
    
    private fun clamp(value: Float, min: Float, max: Float): Float {
        return maxOf(min, minOf(max, value))
    }
    
    // ==================== JSON 序列化 ====================
    
    fun toJSONObject(): org.json.JSONObject {
        val json = org.json.JSONObject()
        json.put("type", type.name)
        json.put("shape", shape.name)
        json.put("scale", scale.toDouble())
        json.put("x", x.toDouble() / inputControlsView.maxWidth)
        json.put("y", y.toDouble() / inputControlsView.maxHeight)
        json.put("toggleSwitch", isToggleSwitch)
        json.put("text", text)
        json.put("iconId", iconId.toInt())
        
        val bindingsArray = org.json.JSONArray()
        for (binding in bindings) bindingsArray.put(binding.name)
        json.put("bindings", bindingsArray)
        
        if (type == Type.CHEAT_CODE_TEXT && cheatCodeText.isNotEmpty() && cheatCodeText != "None") {
            json.put("cheatCodeText", cheatCodeText)
        }
        
        if (!customIconId.isNullOrEmpty()) {
            json.put("customIconId", customIconId)
        }
        
        if (backgroundColor > 0) {
            json.put("backgroundColor", backgroundColor)
        }
        
        if (type == Type.RANGE_BUTTON && range != null) {
            json.put("range", range!!.name)
            if (orientation != 0.toByte()) json.put("orientation", orientation.toInt())
        }
        
        // 保存 Termux 风格额外按键属性
        if (type == Type.EXTRA_KEY && extraKeyName != null) {
            json.put("extraKeyName", extraKeyName)
            if (extraKeyDisplay != null) {
                json.put("extraKeyDisplay", extraKeyDisplay)
            }
            if (extraKeyMacro != null) {
                json.put("extraKeyMacro", extraKeyMacro)
            }
            if (displayStyle != null) {
                json.put("displayStyle", displayStyle)
            }
        }
        
        return json
    }
}

/**
 * CubicBezierInterpolator - 三次贝塞尔曲线插值器
 */
class CubicBezierInterpolator {
    private var mX1 = 0f
    private var mY1 = 0f
    private var mX2 = 0f
    private var mY2 = 0f
    private var mSamples = 200
    private var mCurve = FloatArray(0)
    
    fun set(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (x1 != mX1 || y1 != mY1 || x2 != mX2 || y2 != mY2) {
            mX1 = x1
            mY1 = y1
            mX2 = x2
            mY2 = y2
            mCurve = FloatArray(mSamples)
            for (i in 0 until mSamples) {
                val t = i.toFloat() / (mSamples - 1)
                mCurve[i] = sampleCurveY(sampleCurveX(t))
            }
        }
    }
    
    fun getInterpolation(t: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        val position = (t * (mSamples - 1)).toInt()
        val nextPosition = minOf(position + 1, mSamples - 1)
        val between = (t * (mSamples - 1)) - position
        return mCurve[position] + (mCurve[nextPosition] - mCurve[position]) * between
    }
    
    private fun sampleCurveX(t: Float): Float {
        return ((1 - t) * (1 - t) * (1 - t) * 0 + 3 * (1 - t) * (1 - t) * t * mX1 + 3 * (1 - t) * t * t * mX2 + t * t * t * 1).toFloat()
    }
    
    private fun sampleCurveY(t: Float): Float {
        return ((1 - t) * (1 - t) * (1 - t) * 0 + 3 * (1 - t) * (1 - t) * t * mY1 + 3 * (1 - t) * t * t * mY2 + t * t * t * 1).toFloat()
    }
}
