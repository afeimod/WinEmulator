package org.github.ewt45.winemulator.inputcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Shape
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Type
import kotlin.math.*

/**
 * View for rendering and interacting with input controls
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

    val snappingSize: Int
        get() = if (width > 0) width / 100 else 10

    val maxWidth: Int
        get() = if (snappingSize > 0) (width.toFloat() / snappingSize).roundToInt() * snappingSize else width

    val maxHeight: Int
        get() = if (snappingSize > 0) (height.toFloat() / snappingSize).roundToInt() * snappingSize else height

    private var selectedElement: ControlElement? = null
    private var moveCursor = false
    private var offsetX = 0f
    private var offsetY = 0f
    private val cursor = Point()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var readyToDraw = false

    private var vibrator: Vibrator? = null
    private var vibrationEffect: VibrationEffect? = null

    private var rangeScroller: RangeScroller? = null
    private var currentElementForScroller: ControlElement? = null

    private val primaryColor: Int
        get() = Color.argb((overlayOpacity * 255).toInt(), 255, 255, 255)

    private val secondaryColor: Int
        get() = Color.argb((overlayOpacity * 255).toInt(), 2, 119, 189)

    // 用于检测尺寸变化，重新加载元素坐标
    private var lastMaxWidth = 0
    private var lastMaxHeight = 0

    init {
        setClickable(true)
        setFocusable(true)
        isFocusableInTouchMode = true
        setBackgroundColor(Color.TRANSPARENT)

        try {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        } catch (e: Exception) {
            vibrator = null
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 当视图尺寸变化时，重新加载元素以重新计算坐标
        if (w > 0 && h > 0 && profile != null) {
            if (maxWidth != lastMaxWidth || maxHeight != lastMaxHeight) {
                lastMaxWidth = maxWidth
                lastMaxHeight = maxHeight
                reloadElements()
            }
        }
    }

    private fun reloadElements() {
        if (profile != null) {
            // 保存当前选中的元素（如果有）
            val selected = selectedElement
            // 重新加载元素（会更新所有元素的坐标）
            profile!!.loadElements(this)
            // 恢复选中状态
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
        // 立即加载元素，但可能视图尚未测量，所以等到 onSizeChanged 再实际加载
        if (width > 0 && height > 0) {
            reloadElements()
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
        when {
            binding.isGamepad -> {
                // Gamepad events handled separately
            }
            binding.isMouse -> {
                when (binding) {
                    Binding.MOUSE_LEFT_BUTTON -> inputEventHandler?.onPointerButton(0, isDown)
                    Binding.MOUSE_RIGHT_BUTTON -> inputEventHandler?.onPointerButton(1, isDown)
                    Binding.MOUSE_MIDDLE_BUTTON -> inputEventHandler?.onPointerButton(2, isDown)
                    Binding.MOUSE_MOVE_UP -> if (isDown) inputEventHandler?.onPointerMove(0, -10)
                    Binding.MOUSE_MOVE_DOWN -> if (isDown) inputEventHandler?.onPointerMove(0, 10)
                    Binding.MOUSE_MOVE_LEFT -> if (isDown) inputEventHandler?.onPointerMove(-10, 0)
                    Binding.MOUSE_MOVE_RIGHT -> if (isDown) inputEventHandler?.onPointerMove(10, 0)
                    else -> {}
                }
            }
            binding.isKeyboard -> {
                inputEventHandler?.onKeyEvent(binding.keycode, isDown)
            }
        }
    }

    fun injectPointerMove(dx: Int, dy: Int) {
        inputEventHandler?.onPointerMove(dx, dy)
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
                profile!!.getElements().forEach { it ->
                    drawElement(canvas, it)
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

        val cx = (w * 0.5f).roundToInt().toFloat()
        val cy = (h * 0.5f).roundToInt().toFloat()
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

    private fun drawElement(canvas: Canvas, element: ControlElement) {
        val box = element.getBoundingBox()
        paint.color = if (element.isSelected) secondaryColor else primaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = snappingSize * 0.25f

        when (element.type) {
            Type.BUTTON -> drawButton(canvas, element, box)
            Type.D_PAD -> drawDPad(canvas, element, box)
            Type.STICK -> drawStick(canvas, element, box)
            Type.RANGE_BUTTON -> drawRangeButton(canvas, element, box)
            Type.TRACKPAD -> drawTrackpad(canvas, element, box)
        }
    }

    private fun drawButton(canvas: Canvas, element: ControlElement, box: Rect) {
        val cx = box.centerX().toFloat()
        val cy = box.centerY().toFloat()

        when (element.shape) {
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
                val radius = snappingSize * 0.75f * element.scale
                canvas.drawRoundRect(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat(),
                    radius, radius, paint
                )
            }
        }

        // Draw icon if iconId > 0
        if (element.iconId > 0) {
            drawIcon(canvas, cx, cy, box.width().toFloat(), box.height().toFloat(), element.iconId.toInt(), element.shape, element.scale)
        } else {
            // Draw text
            val text = getDisplayText(element)
            paint.textSize = minOf(
                calculateTextSizeForWidth(paint, text, box.width() - paint.strokeWidth * 2),
                snappingSize * 2 * element.scale
            )
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL
            paint.color = primaryColor
            canvas.drawText(
                text, cx,
                cy - (paint.descent() + paint.ascent()) * 0.5f,
                paint
            )
        }
    }

    private fun drawIcon(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float, iconId: Int, shape: Shape, elementScale: Float = 1.0f) {
        val iconBitmap = getIconBitmap(iconId)
        if (iconBitmap != null) {
            val margin = (snappingSize * (if (shape == Shape.CIRCLE || shape == Shape.SQUARE) 2.0f else 1.0f) * elementScale).toInt()
            val halfSize = ((minOf(width, height) - margin) * 0.5f).toInt()

            val srcRect = Rect(0, 0, iconBitmap.width, iconBitmap.height)
            val dstRect = Rect(
                (cx - halfSize).toInt(),
                (cy - halfSize).toInt(),
                (cx + halfSize).toInt(),
                (cy + halfSize).toInt()
            )

            paint.colorFilter = null
            canvas.drawBitmap(iconBitmap, srcRect, dstRect, paint)
        }
    }

    private var iconCache = mutableMapOf<Int, Bitmap?>()

    private fun getIconBitmap(iconId: Int): Bitmap? {
        if (iconCache.containsKey(iconId)) {
            return iconCache[iconId]
        }

        try {
            context.assets.open("inputcontrols/icons/$iconId.png").use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                iconCache[iconId] = bitmap
                return bitmap
            }
        } catch (e: Exception) {
            // Icon not found, return null
        }
        return null
    }

    private fun drawDPad(canvas: Canvas, element: ControlElement, box: Rect) {
        val cx = box.centerX()
        val cy = box.centerY()
        val offsetX = snappingSize * 2 * element.scale
        val offsetY = snappingSize * 3 * element.scale
        val start = snappingSize * element.scale

        path.reset()

        // Up
        path.moveTo(cx.toFloat(), cy - start)
        path.lineTo(cx - offsetX, cy - offsetY)
        path.lineTo(cx - offsetX.toFloat(), box.top.toFloat())
        path.lineTo((cx + offsetX).toFloat(), box.top.toFloat())
        path.lineTo((cx + offsetX).toFloat(), cy - offsetY.toFloat())
        path.close()

        // Left
        path.moveTo(cx - start, cy.toFloat())
        path.lineTo(cx - offsetY, cy - offsetX)
        path.lineTo(box.left.toFloat(), (cy - offsetX).toFloat())
        path.lineTo(box.left.toFloat(), (cy + offsetX).toFloat())
        path.lineTo(cx - offsetY.toFloat(), cy + offsetX.toFloat())
        path.close()

        // Down
        path.moveTo(cx.toFloat(), cy + start)
        path.lineTo(cx - offsetX, cy + offsetY)
        path.lineTo((cx - offsetX).toFloat(), box.bottom.toFloat())
        path.lineTo((cx + offsetX).toFloat(), box.bottom.toFloat())
        path.lineTo(cx + offsetX, cy + offsetY)
        path.close()

        // Right
        path.moveTo(cx + start, cy.toFloat())
        path.lineTo(cx + offsetY, cy - offsetX)
        path.lineTo(box.right.toFloat(), (cy - offsetX).toFloat())
        path.lineTo(box.right.toFloat(), (cy + offsetX).toFloat())
        path.lineTo(cx + offsetY.toFloat(), cy + offsetX.toFloat())
        path.close()

        canvas.drawPath(path, paint)
    }

    private fun drawStick(canvas: Canvas, element: ControlElement, box: Rect) {
        val cx = box.centerX()
        val cy = box.centerY()

        canvas.drawCircle(cx.toFloat(), cy.toFloat(), box.height() * 0.5f, paint)

        val thumbX = element.x.toFloat()
        val thumbY = element.y.toFloat()
        val thumbRadius = snappingSize * 3.5f * element.scale

        paint.style = Paint.Style.FILL
        paint.color = Color.argb((overlayOpacity * 50).toInt(), 255, 255, 255)
        canvas.drawCircle(thumbX, thumbY, thumbRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.color = if (element.isSelected) secondaryColor else primaryColor
        canvas.drawCircle(thumbX, thumbY, thumbRadius + paint.strokeWidth * 0.5f, paint)
    }

    private fun drawRangeButton(canvas: Canvas, element: ControlElement, box: Rect) {
        val radius = snappingSize * 0.75f * element.scale
        canvas.drawRoundRect(
            box.left.toFloat(), box.top.toFloat(),
            box.right.toFloat(), box.bottom.toFloat(),
            radius, radius, paint
        )
    }

    private fun drawTrackpad(canvas: Canvas, element: ControlElement, box: Rect) {
        val radius = box.height() * 0.15f
        canvas.drawRoundRect(
            box.left.toFloat(), box.top.toFloat(),
            box.right.toFloat(), box.bottom.toFloat(),
            radius, radius, paint
        )

        val offset = paint.strokeWidth * 2.5f
        val innerStrokeWidth = paint.strokeWidth * 2
        val innerHeight = box.height() - offset * 2
        val innerRadius = (innerHeight.toFloat() / box.height()) * radius - (innerStrokeWidth * 0.5f + paint.strokeWidth * 0.5f)

        paint.strokeWidth = innerStrokeWidth
        canvas.drawRoundRect(
            box.left + offset, box.top + offset,
            box.right - offset, box.bottom - offset,
            innerRadius, innerRadius, paint
        )
        paint.strokeWidth = snappingSize * 0.25f
    }

    private fun getDisplayText(element: ControlElement): String {
        if (element.text.isNotEmpty()) {
            return element.text
        }

        val binding = element.getBindingAt(0)
        var text = binding.name.replace("NUMPAD ", "NP").replace("BUTTON ", "")

        if (text.length > 7) {
            val parts = text.split(" ")
            val sb = StringBuilder()
            for (part in parts) {
                if (part.isNotEmpty()) sb.append(part[0])
            }
            return (if (binding.isMouse) "M" else "") + sb
        }
        return text
    }

    private fun calculateTextSizeForWidth(paint: Paint, text: String, desiredWidth: Float): Float {
        val testTextSize = 48f
        paint.textSize = testTextSize
        return testTextSize * desiredWidth / paint.measureText(text)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
                        selectedElement!!.x = ((event.x - offsetX) / snappingSize).roundToInt() * snappingSize
                        selectedElement!!.y = ((event.y - offsetY) / snappingSize).roundToInt() * snappingSize
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (selectedElement != null && profile != null) {
                        profile!!.save()
                    }
                    if (moveCursor) {
                        cursor.x = ((event.x) / snappingSize).roundToInt() * snappingSize
                        cursor.y = ((event.y) / snappingSize).roundToInt() * snappingSize
                    }
                    invalidate()
                }
            }
            return true
        }

        if (!editMode && profile != null) {
            val actionIndex = event.actionIndex
            val pointerId = event.getPointerId(actionIndex)
            val actionMasked = event.actionMasked

            var handled = false

            when (actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)

                    for (element in profile!!.getElements()) {
                        if (element.handleTouchDown(pointerId, x, y)) {
                            vibrator?.vibrate(vibrationEffect)
                            handled = true
                            break
                        }
                    }

                    if (!handled) {
                        // 让 touchpadView 处理（如果存在），但不一定消费事件
                        touchpadView?.onTouchEvent(event)
                        // 注意：即使 touchpadView 处理了，我们也不应该返回 true，因为 X11 仍需接收
                        // 这里 handled 保持 false，以便返回 false 让 X11 处理
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val x = event.getX(i)
                        val y = event.getY(i)
                        val id = event.getPointerId(i)

                        for (element in profile!!.getElements()) {
                            if (element.handleTouchMove(id, x, y)) {
                                handled = true
                                break
                            }
                        }
                    }

                    if (!handled) {
                        touchpadView?.onTouchEvent(event)
                        // 同样，不改变 handled
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    for (element in profile!!.getElements()) {
                        if (element.handleTouchUp(pointerId)) {
                            handled = true
                        }
                    }

                    if (!handled) {
                        touchpadView?.onTouchEvent(event)
                        // 不改变 handled
                    }
                }
            }
            // 只有虚拟按键真正处理了触摸，才返回 true，否则返回 false 让 X11 处理
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
}

/**
 * Touchpad view for mouse simulation
 */
@SuppressLint("ViewConstructor")
class TouchpadView(context: Context) : View(context) {
    var isPointerButtonLeftEnabled = true
        private set

    private var swapMouseButtons = false
    private var simTouchScreen = false

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

    fun setSwapMouseButtons() {
        swapMouseButtons = !swapMouseButtons
    }

    fun setSimTouchScreen() {
        simTouchScreen = !simTouchScreen
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