package org.github.ewt45.winemulator.inputcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
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

    // Fixed: was "snackingSize" (typo), now correct "snappingSize"
    val maxHeight: Int
        get() = if (snappingSize > 0) (height.toFloat() / snappingSize).roundToInt() * snappingSize else height

    private var selectedElement: ControlElement? = null
    private var moveCursor = false
    private var offsetX = 0f
    private var offsetY = 0f
    private val cursor = Point()
    private var pendingProfileReload = false

    // Track which pointers are handled by virtual buttons (persistent across events)
    private val buttonPointers = mutableSetOf<Int>()
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var readyToDraw = false

    private var vibrator: Vibrator? = null
    private var vibrationEffect: VibrationEffect? = null

    private var rangeScroller: RangeScroller? = null
    private var currentElementForScroller: ControlElement? = null

    // Track size changes to reload element positions
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
     * Set X server reference (for calling injectPointerMoveDelta, etc.)
     */
    fun setXServer(server: Any) {
        this.xServer = server
        createMouseMoveTimer()
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
        // If gaming pad binding
        if (binding.isGamepad) {
            // Gaming pad events handled separately
            return
        }

        // Mouse move binding
        if (binding == Binding.MOUSE_MOVE_LEFT || binding == Binding.MOUSE_MOVE_RIGHT) {
            mouseMoveOffset.x = if (isDown) (if (value != 0f) value else (if (binding == Binding.MOUSE_MOVE_LEFT) -1f else 1f)) else 0f
            if (isDown) createMouseMoveTimer()
        } else if (binding == Binding.MOUSE_MOVE_DOWN || binding == Binding.MOUSE_MOVE_UP) {
            mouseMoveOffset.y = if (isDown) (if (value != 0f) value else (if (binding == Binding.MOUSE_MOVE_UP) -1f else 1f)) else 0f
            if (isDown) createMouseMoveTimer()
        } else {
            // Other bindings (keys, mouse buttons)
            when {
                binding.isMouse -> {
                    // Handle mouse button events
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
     * Create mouse move timer - use timer to continuously send mouse move events
     * This solves the continuous movement issue when pressing and holding virtual buttons
     */
    private fun createMouseMoveTimer() {
        // Stop existing timer
        stopMouseMoveTimer()
        
        // Only create timer when there's offset
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

    // Removed gaming pad specific code - these classes don't exist in Linbox project
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Gamepad event handling removed
        return super.onGenericMotionEvent(event)
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
     * This is the core fix for the conflict issue
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

                    // Check virtual buttons for this down position
                    for (element in profile!!.getElements()) {
                        if (element.handleTouchDown(pointerId, x, y)) {
                            handled = true
                            buttonPointers.add(pointerId)
                            // If bound to mouse left button, disable touchpad's left button
                            if (element.getBindingAt(0) == Binding.MOUSE_LEFT_BUTTON) {
                                touchpadView?.setPointerButtonLeftEnabled(false)
                            }
                        }
                    }

                    // If not handled by virtual button, pass to touchpad
                    if (!handled) {
                        touchpadView?.onTouchEvent(event)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    // Iterate all pointers
                    for (i in 0 until event.pointerCount) {
                        val pointerIdI = event.getPointerId(i)
                        val x = event.getX(i)
                        val y = event.getY(i)
                        var thisHandled = false

                        // Check if this pointer is tracked by virtual buttons
                        if (buttonPointers.contains(pointerIdI)) {
                            for (element in profile!!.getElements()) {
                                if (element.handleTouchMove(pointerIdI, x, y)) {
                                    thisHandled = true
                                    break
                                }
                            }
                            // If button no longer handles, remove from tracking
                            if (!thisHandled) {
                                buttonPointers.remove(pointerIdI)
                            }
                        }

                        // If virtual buttons don't handle, pass to touchpad
                        if (!thisHandled && !buttonPointers.contains(pointerIdI)) {
                            touchpadView?.onTouchEvent(event)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    val pointersToHandle = if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
                        // Get all active pointers
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
                        // Remove from tracking
                        buttonPointers.remove(pointerIdUp)
                    }

                    // Pass to touchpad
                    touchpadView?.onTouchEvent(event)
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