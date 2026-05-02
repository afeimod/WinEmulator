package org.github.ewt45.winemulator.inputcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.termux.x11.controller.inputcontrols.GamepadState
import com.termux.x11.controller.xserver.Pointer
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Shape
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Type
import kotlin.math.*

/**
 * View for rendering and interacting with input controls
 * Fixed version that properly handles touch event delegation and mouse move timer
 */
@SuppressLint("ViewConstructor")
class InputControlsView(
    context: Context,
    private var editMode: Boolean = false
) : View(context) {

    var inputEventHandler: InputEventHandler? = null
    var profile: ControlsProfile? = null
        private set
    var showTouchscreenControls = true
    var overlayOpacity = 0.4f

    var touchpadView: TouchpadView? = null
    private var xServer: Any? = null  // Reference to LorieView

    // Timer for continuous mouse movement
    private var mouseMoveTimer: java.util.Timer? = null
    private val mouseMoveOffset = PointF()
    private val cursorSpeed: Float
        get() = profile?.cursorSpeed ?: 1.0f

    val snappingSize: Int
        get() = if (width > 0) maxOf(width, height) / 100 else 10

    val maxWidth: Int
        get() = if (snappingSize > 0) (width.toFloat() / snappingSize).roundToInt() * snappingSize else width

    val maxHeight: Int
        get() = if (snappingSize > 0) (height.toFloat() / snappingSize).roundToInt() * snackingSize else height

    private var selectedElement: ControlElement? = null
    private var moveCursor = false
    private var offsetX = 0f
    private var offsetY = 0f
    private val cursor = Point()
    private var pendingProfileReload = false

    // 追踪哪些pointer被虚拟按键处理（跨事件持久化）
    private val buttonPointers = mutableSetOf<Int>()
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var readyToDraw = false

    private var vibrator: Vibrator? = null
    private var vibrationEffect: VibrationEffect? = null

    private var rangeScroller: RangeScroller? = null
    private var currentElementForScroller: ControlElement? = null

    // 用于检测尺寸变化，重新加载元素坐标
    private var lastMaxWidth = 0
    private var lastMaxHeight = 0

    init {
        setClickable(true)
        setFocusable(true)
        isFocusableInTouchMode = true
        setBackgroundColor(Color.TRANSPARENT)
        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        @Suppress("DEPRECATION")
        try {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        } catch (e: Exception) {
            vibrator = null
        }
    }

    /**
     * 设置X服务器引用（用于调用injectPointerMoveDelta等方法）
     */
    fun setXServer(server: Any) {
        this.xServer = server
        createMouseMoveTimer()
    }

    /**
     * 获取X服务器引用
     */
    fun getXServer(): Any? = xServer

    /**
     * 设置是否显示虚拟按键，同时调整视图的点击和聚焦状态
     */
    @JvmName("setControlsVisible")
    fun setControlsVisible(show: Boolean) {
        showTouchscreenControls = show
        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            if (pendingProfileReload && profile != null) {
                pendingProfileReload = false
                reloadElements()
            } else if (profile != null) {
                reloadElements()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (profile != null && width > 0 && height > 0) {
            reloadElements()
        }
    }

    private fun reloadElements() {
        if (profile != null) {
            val selected = selectedElement
            profile!!.loadElements(this)
            if (selected != null) {
                val newSelected = profile!!.getElements().find { it == selected }
                selectElement(newSelected)
            }
        }
        invalidate()
    }

    fun setEditMode(mode: Boolean) {
        editMode = mode
    }

    fun setProfile(profile: ControlsProfile?) {
        this.profile = profile
        deselectAllElements()
        stopMouseMoveTimer()
        if (width > 0 && height > 0) {
            pendingProfileReload = false
            reloadElements()
        } else {
            pendingProfileReload = true
        }
    }

    fun getSelectedElement(): ControlElement? = selectedElement

    fun addElement(): Boolean {
        if (editMode && profile != null) {
            val element = ControlElement(this)
            element.x = cursor.x
            element.y = cursor.y
            element.initDefaultBindings()
            profile!!.addElement(element)
            profile!!.save()
            selectElement(element)
            return true
        }
        return false
    }

    fun removeElement(): Boolean {
        if (editMode && selectedElement != null && profile != null) {
            profile!!.removeElement(selectedElement!!)
            selectedElement = null
            profile!!.save()
            invalidate()
            return true
        }
        return false
    }

    fun getRangeScroller(): RangeScroller? = rangeScroller

    private fun deselectAllElements() {
        selectedElement = null
        profile?.getElements()?.forEach { it.isSelected = false }
    }

    private fun selectElement(element: ControlElement?) {
        deselectAllElements()
        if (element != null) {
            selectedElement = element
            element.isSelected = true
        }
        invalidate()
    }

    fun handleInputEvent(binding: Binding, isDown: Boolean, value: Float = 0f) {
        // 如果绑定了游戏手柄
        if (binding.isGamepad) {
            // Gamepad events handled separately - would need winHandler integration
            return
        }

        // 鼠标移动绑定
        if (binding == Binding.MOUSE_MOVE_LEFT || binding == Binding.MOUSE_MOVE_RIGHT) {
            mouseMoveOffset.x = if (isDown) (if (value != 0f) value else (if (binding == Binding.MOUSE_MOVE_LEFT) -1f else 1f)) else 0f
            if (isDown) createMouseMoveTimer()
        } else if (binding == Binding.MOUSE_MOVE_DOWN || binding == Binding.MOUSE_MOVE_UP) {
            mouseMoveOffset.y = if (isDown) (if (value != 0f) value else (if (binding == Binding.MOUSE_MOVE_UP) -1f else 1f)) else 0f
            if (isDown) createMouseMoveTimer()
        } else {
            // 其他绑定（按键、鼠标按钮）
            when {
                binding.isMouse -> {
                    // 处理鼠标按钮事件
                    binding.getPointerButton()?.let { button ->
                        inputEventHandler?.onPointerButton(button, isDown)
                    }
                }
                binding.isKeyboard -> {
                    inputEventHandler?.onKeyEvent(binding.keycode, isDown)
                }
            }
        }
    }

    fun injectPointerMove(dx: Int, dy: Int) {
        inputEventHandler?.onPointerMove(dx, dy)
    }

    /**
     * 创建鼠标移动定时器 - 使用定时器持续发送鼠标移动事件
     * 这解决了虚拟按键长按时持续移动的问题
     */
    private fun createMouseMoveTimer() {
        // 停止已有的定时器
        stopMouseMoveTimer()
        
        // 只有在有偏移量时才创建定时器
        if (mouseMoveOffset.x == 0f && mouseMoveOffset.y == 0f) return
        if (profile == null) return
        
        mouseMoveTimer = java.util.Timer()
        mouseMoveTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                val speed = cursorSpeed
                val dx = (mouseMoveOffset.x * 10 * speed).toInt()
                val dy = (mouseMoveOffset.y * 10 * speed).toInt()
                if (dx != 0 || dy != 0) {
                    injectPointerMove(dx, dy)
                }
            }
        }, 0, 1000 / 60) // 60fps
    }

    /**
     * 停止鼠标移动定时器
     */
    private fun stopMouseMoveTimer() {
        mouseMoveTimer?.cancel()
        mouseMoveTimer = null
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height

        if (w == 0 || h == 0) {
            readyToDraw = false
            return
        }

        readyToDraw = true

        if (editMode) {
            drawGrid(canvas)
            drawCursor(canvas)
        }

        if (profile != null) {
            if (!profile!!.isElementsLoaded()) {
                reloadElements()
            }
            if (showTouchscreenControls) {
                profile!!.getElements().forEach { element ->
                    element.draw(canvas)
                }
            }
        }

        super.onDraw(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = snappingSize * 0.0625f
        paint.color = Color.BLACK
        canvas.drawColor(Color.BLACK)

        paint.isAntiAlias = false
        paint.color = Color.rgb(48, 48, 48)

        val w = maxWidth
        val h = maxHeight

        var i = 0
        while (i <= w) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), h.toFloat(), paint)
            i += snappingSize
        }
        i = 0
        while (i <= h) {
            canvas.drawLine(0f, i.toFloat(), w.toFloat(), i.toFloat(), paint)
            i += snappingSize
        }

        val cx = roundTo(w * 0.5f, snappingSize.toFloat())
        val cy = roundTo(h * 0.5f, snappingSize.toFloat())
        paint.color = Color.rgb(66, 66, 66)

        i = 0
        while (i <= w) {
            canvas.drawLine(cx, i.toFloat(), cx, (i + snappingSize).toFloat(), paint)
            i += snappingSize * 2
        }
        i = 0
        while (i <= h) {
            canvas.drawLine(i.toFloat(), cy, (i + snappingSize).toFloat(), cy, paint)
            i += snappingSize * 2
        }

        paint.isAntiAlias = true
    }

    private fun drawCursor(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = snappingSize * 0.0625f
        paint.color = Color.rgb(198, 40, 40)

        paint.isAntiAlias = false
        canvas.drawLine(0f, cursor.y.toFloat(), maxWidth.toFloat(), cursor.y.toFloat(), paint)
        canvas.drawLine(cursor.x.toFloat(), 0f, cursor.x.toFloat(), maxHeight.toFloat(), paint)
        paint.isAntiAlias = true
    }

    fun getPaint(): Paint = paint

    fun getPath(): Path = path

    fun getColorFilter(): ColorFilter {
        return PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
    }

    fun getPrimaryColor(): Int = Color.argb((overlayOpacity * 255).toInt(), 255, 255, 255)

    fun getSecondaryColor(): Int = Color.argb((overlayOpacity * 255).toInt(), 2, 119, 189)

    fun getHighlightColor(): Int = Color.argb((overlayOpacity * 255).toInt(), 255, 193, 7)

    fun getIcon(id: Byte): Bitmap? {
        if (icons[id.toInt()] == null) {
            try {
                context.assets.open("inputcontrols/icons/$id.png").use { inputStream ->
                    icons[id.toInt()] = BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                // Icon not found
            }
        }
        return icons[id.toInt()]
    }

    // Icon cache
    private val icons = arrayOfNulls<Bitmap>(17)

    private fun roundTo(value: Float, rounding: Float): Float {
        return (value / rounding).roundToInt() * rounding
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // 处理外部游戏手柄事件
        if (!editMode && profile != null) {
            val controller = profile!!.getController(event.deviceId)
            if (controller != null && controller.updateStateFromMotionEvent(event)) {
                processJoystickInput(controller)
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        // 将悬停事件传递给touchpad
        return touchpadView?.onHoverEvent(event) ?: false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 编辑模式下，由父类处理触摸事件
        if (editMode && readyToDraw) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y

                    val element = intersectElement(x, y)
                    moveCursor = true

                    if (element != null) {
                        offsetX = x - element.x
                        offsetY = y - element.y
                        moveCursor = false
                    }

                    selectElement(element)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (selectedElement != null) {
                        selectedElement!!.x = roundTo(event.x - offsetX, snappingSize.toFloat()).toInt()
                        selectedElement!!.y = roundTo(event.y - offsetY, snappingSize.toFloat()).toInt()
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (selectedElement != null && profile != null) {
                        profile!!.save()
                    }
                    if (moveCursor) {
                        cursor.x = roundTo(event.x, snappingSize.toFloat()).toInt()
                        cursor.y = roundTo(event.y, snappingSize.toFloat()).toInt()
                    }
                    invalidate()
                }
            }
            return true
        }

        // 非编辑模式，使用handleTouchEvent处理
        return handleTouchEvent(event)
    }

    /**
     * 处理触摸事件 - 这是与termux-x11保持一致的核心方法
     * 正确处理虚拟按键和touchpad之间的触摸分发
     */
    fun handleTouchEvent(event: MotionEvent): Boolean {
        // 鼠标事件直接传递给touchpad
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            return touchpadView?.onTouchEvent(event) ?: false
        }

        // 非编辑模式且有profile时处理触摸
        if (!editMode && profile != null) {
            val actionIndex = event.actionIndex
            val pointerId = event.getPointerId(actionIndex)
            val actionMasked = event.actionMasked
            var handled = false

            when (actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)
                    
                    // 启用左键
                    touchpadView?.setPointerButtonLeftEnabled(true)

                    // 遍历虚拟按键检查这个down位置
                    for (element in profile!!.getElements()) {
                        if (element.handleTouchDown(pointerId, x, y)) {
                            handled = true
                            buttonPointers.add(pointerId)
                            // 如果绑定的是鼠标左键，禁用touchpad的左键
                            if (element.getBindingAt(0) == Binding.MOUSE_LEFT_BUTTON) {
                                touchpadView?.setPointerButtonLeftEnabled(false)
                            }
                        }
                    }

                    // 如果没有被虚拟按键处理，传递给touchpad
                    if (!handled) {
                        touchpadView?.onTouchEvent(event)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    // 遍历所有pointers
                    for (i in 0 until event.pointerCount) {
                        val pointerIdI = event.getPointerId(i)
                        val x = event.getX(i)
                        val y = event.getY(i)
                        var thisHandled = false

                        // 检查这个pointer是否被虚拟按键追踪
                        if (buttonPointers.contains(pointerIdI)) {
                            for (element in profile!!.getElements()) {
                                if (element.handleTouchMove(pointerIdI, x, y)) {
                                    thisHandled = true
                                    break
                                }
                            }
                            // 如果虚拟按键不再处理，从追踪中移除
                            if (!thisHandled) {
                                buttonPointers.remove(pointerIdI)
                            }
                        }

                        // 如果虚拟按键没有处理，传递给touchpad
                        if (!thisHandled && !buttonPointers.contains(pointerIdI)) {
                            touchpadView?.onTouchEvent(event)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    val pointersToHandle = if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
                        // 获取所有活动的pointer
                        (0 until event.pointerCount).map { event.getPointerId(it) }.toSet()
                    } else {
                        setOf(event.getPointerId(event.actionIndex))
                    }

                    for (pointerIdUp in pointersToHandle) {
                        for (element in profile!!.getElements()) {
                            if (element.handleTouchUp(pointerIdUp)) {
                                handled = true
                            }
                        }
                        // 从追踪中移除
                        buttonPointers.remove(pointerIdUp)
                    }

                    // 传递给touchpad
                    touchpadView?.onTouchEvent(event)
                }
            }

            return handled
        }
        return false
    }

    /**
     * 处理游戏手柄输入
     */
    private fun processJoystickInput(controller: com.termux.x11.controller.inputcontrols.ExternalController) {
        val axes = intArrayOf(
            MotionEvent.AXIS_X, MotionEvent.AXIS_Y, 
            MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y
        )
        val values = floatArrayOf(
            controller.state.thumbLX, controller.state.thumbLY,
            controller.state.thumbRX, controller.state.thumbRY,
            controller.state.getDPadX(), controller.state.getDPadY()
        )

        for (i in axes.indices) {
            if (abs(values[i]) > ControlElement.STICK_DEAD_ZONE) {
                val controllerBinding = controller.getControllerBinding(
                    com.termux.x11.controller.inputcontrols.ExternalControllerBinding.getKeyCodeForAxis(axes[i], Mathf.sign(values[i]))
                )
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding, true, values[i])
                }
            } else {
                val positiveBinding = controller.getControllerBinding(
                    com.termux.x11.controller.inputcontrols.ExternalControllerBinding.getKeyCodeForAxis(axes[i], 1.toByte())
                )
                if (positiveBinding != null) {
                    handleInputEvent(positiveBinding.binding, false, values[i])
                }
                val negativeBinding = controller.getControllerBinding(
                    com.termux.x11.controller.inputcontrols.ExternalControllerBinding.getKeyCodeForAxis(axes[i], (-1).toByte())
                )
                if (negativeBinding != null) {
                    handleInputEvent(negativeBinding.binding, false, values[i])
                }
            }
        }
    }

    private fun intersectElement(x: Float, y: Float): ControlElement? {
        profile?.getElements()?.forEach { element ->
            if (element.containsPoint(x, y)) return element
        }
        return null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopMouseMoveTimer()
    }
}

/**
 * Touchpad view for mouse simulation - simplified version for Android
 */
@SuppressLint("ViewConstructor")
class TouchpadView(context: Context) : View(context) {
    var isPointerButtonLeftEnabled = true
        private set

    private var lastX = 0f
    private var lastY = 0f

    var inputEventHandler: InputEventHandler? = null

    companion object {
        const val CURSOR_ACCELERATION = 2f
        const val CURSOR_ACCELERATION_THRESHOLD = 4f
    }

    fun setPointerButtonLeftEnabled(enabled: Boolean) {
        isPointerButtonLeftEnabled = enabled
    }

    fun computeDeltaPoint(oldX: Float, oldY: Float, newX: Float, newY: Float): FloatArray {
        return floatArrayOf(newX - oldX, newY - oldY)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY

                if (abs(dx) > CURSOR_ACCELERATION_THRESHOLD || abs(dy) > CURSOR_ACCELERATION_THRESHOLD) {
                    inputEventHandler?.onPointerMove(
                        (dx * CURSOR_ACCELERATION).toInt(),
                        (dy * CURSOR_ACCELERATION).toInt()
                    )
                } else {
                    inputEventHandler?.onPointerMove(dx.toInt(), dy.toInt())
                }

                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }
}