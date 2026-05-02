package org.github.ewt45.winemulator.inputcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Timer
import java.util.TimerTask
import kotlin.math.*

/**
 * Complete InputControlsView implementation based on termux-app
 * Fixed version with complete feature set and bug fixes
 */
@SuppressLint("ViewConstructor")
class InputControlsView(
    context: Context,
    private var editMode: Boolean = false
) : View(context) {

    companion object {
        const val DEFAULT_OVERLAY_OPACITY = 0.4f
        const val MAX_TAP_TRAVEL_DISTANCE = 10
        const val MAX_TAP_MILLISECONDS = 200
        const val CURSOR_ACCELERATION = 1.25f
        const val CURSOR_ACCELERATION_THRESHOLD = 6
        const val CLICK_MAX_DISTANCE = 10f
        const val CLICK_MAX_TIME = 200L
    }

    var inputEventHandler: InputEventHandler? = null
    var profile: ControlsProfile? = null
        private set
    var showTouchscreenControls = true
    var overlayOpacity = DEFAULT_OVERLAY_OPACITY

    var touchpadView: TouchpadView? = null
    private var xServer: Any? = null  // Reference to LorieView

    // Timer for continuous mouse movement
    private var mouseMoveTimer: Timer? = null
    private val mouseMoveOffset = PointF()
    private val cursor = Point()
    private val cursorSpeed: Float
        get() = profile?.cursorSpeed ?: 1.0f

    val snappingSize: Int
        get() = if (width > 0) maxOf(width, height) / 100 else 10

    val maxWidth: Int
        get() = if (snappingSize > 0) (width.toFloat() / snappingSize).roundToInt() * snappingSize else width

    val maxHeight: Int
        get() = if (snappingSize > 0) (height.toFloat() / snappingSize).roundToInt() * snappingSize else height

    private var selectedElement: ControlElement? = null
    private var moveCursor = false
    private var offsetX = 0f
    private var offsetY = 0f
    private var pendingProfileReload = false

    // Track which pointers are handled by virtual buttons (persistent across events)
    private val buttonPointers = mutableSetOf<Int>()
    
    // 跟踪触控板使用的触点ID及其最后位置
    private val touchpadPointers = mutableMapOf<Int, PointF>()
    
    // 跟踪触点的按下时间和位置，用于检测点击
    private data class TouchDownInfo(
        val downTime: Long,
        val downPosition: PointF,
        var isUp: Boolean = false
    )
    private val touchDownInfos = mutableMapOf<Int, TouchDownInfo>()
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val colorFilter = PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
    private var readyToDraw = false

    private var vibrator: Vibrator? = null
    private var vibrationEffect: VibrationEffect? = null

    // Icon cache - complete 17 icons like termux-app
    private val icons = arrayOfNulls<Bitmap>(17)
    
    // Counter map for tracking icon usage (termux-app feature)
    private val counterMap = mutableMapOf<String, Int>()

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

    fun setXServer(server: Any) {
        this.xServer = server
        createMouseMoveTimer()
    }
    
    // 获取触控板指针的最后位置
    fun getTouchpadLastPosition(pointerId: Int): PointF? = touchpadPointers[pointerId]
    
    // 更新触控板指针的最后位置
    fun updateTouchpadLastPosition(pointerId: Int, x: Float, y: Float) {
        touchpadPointers[pointerId] = PointF(x, y)
    }
    
    // 移除触控板指针
    fun removeTouchpadPointer(pointerId: Int) {
        touchpadPointers.remove(pointerId)
        touchDownInfos.remove(pointerId)
    }

    /**
     * Get X server reference
     */
    fun getXServer(): Any? = xServer

    /**
     * Set whether to show virtual controls, and adjust view's click/focus state
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

    fun getRangeScroller(): RangeScroller? = null // RangeScroller is managed by ControlElement

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

    /**
     * Complete handleInputEvent implementation based on termux-app
     */
    fun handleInputEvent(binding: Binding, isDown: Boolean) {
        handleInputEvent(binding, isDown, 0f)
    }

    fun handleInputEvent(binding: Binding, isDown: Boolean, offset: Float) {
        // If gaming pad binding
        if (binding.isGamepad) {
            // Gaming pad events handled separately through processJoystickInput
            // But we can handle gamepad button state changes here if needed
            return
        }

        // Mouse move binding
        if (binding == Binding.MOUSE_MOVE_LEFT || binding == Binding.MOUSE_MOVE_RIGHT) {
            mouseMoveOffset.x = if (isDown) (if (offset != 0f) offset else (if (binding == Binding.MOUSE_MOVE_LEFT) -1f else 1f)) else 0f
            if (isDown) createMouseMoveTimer()
        } else if (binding == Binding.MOUSE_MOVE_DOWN || binding == Binding.MOUSE_MOVE_UP) {
            mouseMoveOffset.y = if (isDown) (if (offset != 0f) offset else (if (binding == Binding.MOUSE_MOVE_UP) -1f else 1f)) else 0f
            if (isDown) createMouseMoveTimer()
        } else {
            // Other bindings (keys, mouse buttons)
            when {
                binding.isMouse -> {
                    // Handle mouse button events
                    val pointerButton = binding.getPointerButton()
                    if (pointerButton != null) {
                        if (isDown) {
                            inputEventHandler?.onPointerButton(pointerButton - 1, true)
                        } else {
                            inputEventHandler?.onPointerButton(pointerButton - 1, false)
                        }
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
     * Create mouse move timer - use timer to continuously send mouse move events
     * This solves the continuous movement issue when pressing and holding virtual buttons
     */
    private fun createMouseMoveTimer() {
        // Stop existing timer
        stopMouseMoveTimer()
        
        // Only create timer when there's offset
        if (mouseMoveOffset.x == 0f && mouseMoveOffset.y == 0f) return
        if (profile == null) return
        
        mouseMoveTimer = Timer()
        mouseMoveTimer?.scheduleAtFixedRate(object : TimerTask() {
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
     * Stop mouse move timer
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

    fun getColorFilter(): ColorFilter = colorFilter

    fun getPrimaryColor(): Int = Color.argb((overlayOpacity * 255).toInt(), 255, 255, 255)

    fun getSecondaryColor(): Int = Color.argb((overlayOpacity * 255).toInt(), 2, 119, 189)

    fun getHighlightColor(): Int = Color.argb((overlayOpacity * 255).toInt(), 255, 193, 7)

    /**
     * Get icon by ID - loads from assets
     */
    fun getIcon(id: Byte): Bitmap? {
        if (id < 0 || id >= icons.size) return null
        
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

    /**
     * Get custom icon from app's private storage
     */
    fun getCustomIcon(iconId: String): Bitmap? {
        val buttonIconFile = File(context.filesDir.path + "/home/.buttonIcons", "$iconId.png")
        if (!buttonIconFile.exists()) {
            return null
        }
        return BitmapFactory.decodeFile(buttonIconFile.path)
    }

    /**
     * Clip bitmap to circular or rectangular shape
     */
    fun clipBitmap(bitmap: Bitmap?, isCircular: Boolean): Bitmap? {
        if (bitmap == null) return null
        
        val clippedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(clippedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        
        if (isCircular) {
            val centerX = bitmap.width / 2
            val centerY = bitmap.height / 2
            val radius = minOf(centerX, centerY)
            canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), paint)
        } else {
            val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            canvas.drawRect(rect, paint)
        }
        return clippedBitmap
    }

    /**
     * Create shape bitmap (circle or rectangle) with specified color
     */
    fun createShapeBitmap(width: Float, height: Float, color: Int, isCircular: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color

        if (isCircular) {
            val radius = (minOf(width, height) / 2).toInt()
            canvas.drawCircle(width / 2, height / 2, radius.toFloat(), paint)
        } else {
            val rect = RectF(0f, 0f, width, height)
            canvas.drawRect(rect, paint)
        }
        return bitmap
    }

    /**
     * Counter map operations for icon tracking (termux-app feature)
     */
    fun counterMapIncrease(iconId: String) {
        val v = counterMap[iconId] ?: 0
        counterMap[iconId] = v + 1
    }

    fun counterMapDecrease(iconId: String) {
        val v = counterMap[iconId]
        if (v != null) {
            counterMap[iconId] = v - 1
        }
    }

    fun counterMapZero(iconId: String): Boolean {
        val v = counterMap[iconId]
        return v == null || v <= 0
    }

    private fun roundTo(value: Float, rounding: Float): Float {
        return (value / rounding).roundToInt() * rounding
    }

    /**
     * Handle external gamepad events
     */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Process external gamepad/controller events
        if (!editMode && profile != null) {
            val controller = profile!!.getController(event.deviceId)
            if (controller != null && controller.updateStateFromMotionEvent(event)) {
                // Handle L2/R2 buttons (termux-app feature)
                processGamepadButtons(controller)
                processJoystickInput(controller)
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    /**
     * Process gamepad button states (L2/R2 triggers)
     */
    private fun processGamepadButtons(controller: ExternalController) {
        val state = controller.state
        
        // L2 trigger
        val l2Binding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_L2)
        if (l2Binding?.binding != null) {
            handleInputEvent(l2Binding.binding!!, state.isPressed(ExternalController.IDX_BUTTON_L2.toInt()))
        }
        
        // R2 trigger
        val r2Binding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_R2)
        if (r2Binding?.binding != null) {
            handleInputEvent(r2Binding.binding!!, state.isPressed(ExternalController.IDX_BUTTON_R2.toInt()))
        }
    }

    /**
     * Process joystick input and send key events
     */
    private fun processJoystickInput(controller: ExternalController) {
        val axes = intArrayOf(
            MotionEvent.AXIS_X, MotionEvent.AXIS_Y, 
            MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y
        )
        val values = floatArrayOf(
            controller.state.thumbLX, controller.state.thumbLY,
            controller.state.thumbRX, controller.state.thumbRY,
            controller.state.getDPadX().toFloat(), controller.state.getDPadY().toFloat()
        )

        for (i in axes.indices) {
            if (abs(values[i]) > ControlElement.STICK_DEAD_ZONE) {
                val direction: Int = if (values[i] > 0) 1 else -1
                val controllerBinding = controller.getControllerBinding(
                    ExternalControllerBinding.getKeyCodeForAxis(axes[i], direction)
                )
                if (controllerBinding?.binding != null) {
                    handleInputEvent(controllerBinding.binding!!, true, values[i])
                }
            } else {
                val positiveBinding = controller.getControllerBinding(
                    ExternalControllerBinding.getKeyCodeForAxis(axes[i], 1)
                )
                if (positiveBinding?.binding != null) {
                    handleInputEvent(positiveBinding.binding!!, false, values[i])
                }
                val negativeBinding = controller.getControllerBinding(
                    ExternalControllerBinding.getKeyCodeForAxis(axes[i], -1)
                )
                if (negativeBinding?.binding != null) {
                    handleInputEvent(negativeBinding.binding!!, false, values[i])
                }
            }
        }
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        // Pass hover events to touchpad
        return touchpadView?.onHoverEvent(event) ?: false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // In edit mode, parent handles touch events
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

        // In non-edit mode, use handleTouchEvent
        return handleTouchEvent(event)
    }

    /**
     * Handle touch events - correctly dispatch between virtual buttons and touchpad
     * Complete implementation based on termux-app
     */
    fun handleTouchEvent(event: MotionEvent): Boolean {
        // Mouse events directly to touchpad
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            return touchpadView?.onTouchEvent(event) ?: false
        }

        // Non-edit mode with profile, handle touch
        if (!editMode && profile != null) {
            val actionIndex = event.actionIndex
            val pointerId = event.getPointerId(actionIndex)
            val actionMasked = event.actionMasked
            var handled = false

            when (actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)
                    
                    // Enable left button
                    touchpadView?.setPointerButtonLeftEnabled(true)

                    var handledByControl = false
                    // Check virtual buttons for this down position
                    for (element in profile!!.getElements()) {
                        if (element.handleTouchDown(pointerId, x, y)) {
                            vibrator?.vibrate(vibrationEffect)
                            // 记录该触点已被占用
                            buttonPointers.add(pointerId)
                            handledByControl = true
                            // If bound to mouse left button, disable touchpad's left button
                            if (element.getBindingAt(0) == Binding.MOUSE_LEFT_BUTTON) {
                                touchpadView?.setPointerButtonLeftEnabled(false)
                            }
                            break
                        }
                    }

                    if (!handledByControl) {
                        // 这个触点没有被控件占用，可以用于触控板
                        touchpadPointers[pointerId] = PointF(x, y)
                        // 记录按下时间和位置，用于检测点击
                        touchDownInfos[pointerId] = TouchDownInfo(System.currentTimeMillis(), PointF(x, y))
                    }
                    // DOWN 事件总是被处理
                    handled = true
                }
                MotionEvent.ACTION_MOVE -> {
                    // 遍历所有触点，独立处理每个触点的移动
                    for (i in 0 until event.pointerCount) {
                        val x = event.getX(i)
                        val y = event.getY(i)
                        val id = event.getPointerId(i)

                        // 如果该触点已被某个控件占用，则交给控件处理
                        if (buttonPointers.contains(id)) {
                            var thisHandled = false
                            for (element in profile!!.getElements()) {
                                if (element.handleTouchMove(id, x, y)) {
                                    thisHandled = true
                                    handled = true
                                    break
                                }
                            }
                            // 对于按钮等不处理MOVE的控件，保持追踪，不移除
                        } else {
                            // 这个触点没有被控件占用，作为触控板处理
                            val lastPos = touchpadPointers[id]
                            
                            if (lastPos != null) {
                                // 计算相对于上次位置的增量移动
                                val dx = x - lastPos.x
                                val dy = y - lastPos.y
                                
                                // 直接调用输入事件处理器
                                if (abs(dx) > CURSOR_ACCELERATION_THRESHOLD || abs(dy) > CURSOR_ACCELERATION_THRESHOLD) {
                                    inputEventHandler?.onPointerMove(
                                        (dx * CURSOR_ACCELERATION).toInt(),
                                        (dy * CURSOR_ACCELERATION).toInt()
                                    )
                                } else {
                                    inputEventHandler?.onPointerMove(dx.toInt(), dy.toInt())
                                }
                                
                                // 更新最后位置
                                lastPos.set(x, y)
                                handled = true
                            } else {
                                // 第一次看到这个未占用的触点，记录初始位置但不产生移动
                                touchpadPointers[id] = PointF(x, y)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    var handledByControl = false
                    for (element in profile!!.getElements()) {
                        if (element.handleTouchUp(pointerId)) {
                            handledByControl = true
                        }
                    }

                    if (handledByControl) {
                        // 释放该触点的占用状态
                        buttonPointers.remove(pointerId)
                    } else {
                        // 这个触点是用于触控板的，移除它
                        touchpadPointers.remove(pointerId)
                        touchDownInfos.remove(pointerId)
                    }
                }
            }

            return handled
        }
        return false
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
 * Touchpad view for mouse simulation - complete version with tap detection
 * Based on termux-app implementation
 */
@SuppressLint("ViewConstructor")
class TouchpadView(context: Context) : View(context) {
    var isPointerButtonLeftEnabled = true
        private set

    private var lastX = 0f
    private var lastY = 0f

    // Finger tracking for tap detection
    private var fingerStartX = 0f
    private var fingerStartY = 0f
    private var fingerStartTime = 0L
    private var isFingerDown = false
    
    var inputEventHandler: InputEventHandler? = null

    fun setPointerButtonLeftEnabled(enabled: Boolean) {
        isPointerButtonLeftEnabled = enabled
    }

    fun computeDeltaPoint(oldX: Float, oldY: Float, newX: Float, newY: Float): FloatArray {
        return floatArrayOf(newX - oldX, newY - oldY)
    }

    /**
     * Check if the finger movement qualifies as a tap (quick light touch)
     */
    private fun isTap(): Boolean {
        if (!isFingerDown) return false
        
        val touchDuration = System.currentTimeMillis() - fingerStartTime
        val travelDistance = kotlin.math.sqrt(
            (lastX - fingerStartX) * (lastX - fingerStartX) + 
            (lastY - fingerStartY) * (lastY - fingerStartY)
        )
        
        return touchDuration < InputControlsView.MAX_TAP_MILLISECONDS * 5 && travelDistance < InputControlsView.MAX_TAP_TRAVEL_DISTANCE * 5
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionMasked = event.actionMasked
        
        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Mark finger as down and record start position/time
                isFingerDown = true
                fingerStartX = event.x
                fingerStartY = event.y
                lastX = fingerStartX
                lastY = fingerStartY
                fingerStartTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                
                // Update position
                lastX = event.x
                lastY = event.y
                
                if (abs(dx) > InputControlsView.CURSOR_ACCELERATION_THRESHOLD || abs(dy) > InputControlsView.CURSOR_ACCELERATION_THRESHOLD) {
                    inputEventHandler?.onPointerMove(
                        (dx * InputControlsView.CURSOR_ACCELERATION).toInt(),
                        (dy * InputControlsView.CURSOR_ACCELERATION).toInt()
                    )
                } else {
                    inputEventHandler?.onPointerMove(dx.toInt(), dy.toInt())
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                // On finger up, check if it was a tap and send mouse click
                if (isFingerDown && isTap() && isPointerButtonLeftEnabled) {
                    // Send mouse button press (left click down)
                    inputEventHandler?.onPointerButton(0, true)  // 0 = left button
                    
                    // Send mouse button release after a short delay
                    postDelayed({
                        inputEventHandler?.onPointerButton(0, false)
                    }, 30)
                }
                
                // Reset finger state
                isFingerDown = false
            }
        }
        return true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        // Basic hover support
        return false
    }
}
