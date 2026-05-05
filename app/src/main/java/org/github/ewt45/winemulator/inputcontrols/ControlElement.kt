package org.github.ewt45.winemulator.inputcontrols

import android.graphics.*
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Range
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Shape
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Type
import kotlin.math.*

class ControlElement(
    private val inputControlsView: InputControlsView
) {
    private var activePointerId = -1

    companion object {
        const val STICK_DEAD_ZONE = 0.15f
        const val DPAD_DEAD_ZONE = 0.3f
        const val STICK_SENSITIVITY = 3.0f
        const val TRACKPAD_MIN_SPEED = 0.8f
        const val TRACKPAD_MAX_SPEED = 20.0f
        const val TRACKPAD_ACCELERATION_THRESHOLD: Byte = 4
        const val BUTTON_MIN_TIME_TO_KEEP_PRESSED: Short = 0
        
    }

    enum class Type {
        BUTTON,
        D_PAD,
        RANGE_BUTTON,
        STICK,
        TRACKPAD,
        COMBINE_BUTTON,
        CHEAT_CODE_TEXT;

        companion object {
            fun names(): Array<String> = entries.map { it.name.replace("_", "-") }.toTypedArray()
        }
    }

    enum class Shape {
        CIRCLE,
        RECT,
        ROUND_RECT,
        SQUARE;

        companion object {
            fun names(): Array<String> = entries.map { it.name.replace("_", " ") }.toTypedArray()
        }
    }

    enum class Range(val max: Byte) {
    private var activePointerId = -1

        FROM_A_TO_Z(26),
        FROM_0_TO_9(10),
        FROM_F1_TO_F12(12),
        FROM_NP0_TO_NP9(10);

        companion object {
            fun names(): Array<String> = entries.map { it.name.replace("_", " ") }.toTypedArray()
            fun fromString(name: String): Range? {
                return when (name) {
    private var activePointerId = -1

                    "FROM_A_TO_Z", "A-Z", "FROM-A-TO-Z" -> FROM_A_TO_Z
                    "FROM_0_TO_9", "0-9", "DIGITS", "FROM-0-TO-9" -> FROM_0_TO_9
                    "FROM_F1_TO_F12", "F1-F12", "FUNCTION_KEYS", "FROM-F1-TO-F12" -> FROM_F1_TO_F12
                    "FROM_NP0_TO_NP9", "NP0-NP9", "NUMPAD_DIGITS", "FROM-NP0-TO-NP9" -> FROM_NP0_TO_NP9
                    else -> null
                }
            }
        }
    }

    var type: Type = Type.BUTTON
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                reset()
            }
        }
    var shape: Shape = Shape.CIRCLE
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    private var bindings: Array<Binding> = arrayOf(Binding.NONE, Binding.NONE, Binding.NONE, Binding.NONE)
    var scale: Float = 1.0f
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    var x: Int = 0
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    var y: Int = 0
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    var isSelected: Boolean = false
    var isToggleSwitch: Boolean = false
    var text: String = ""
        set(value) {
    private var activePointerId = -1

            field = value
        }
    var iconId: Byte = 0
        set(value) {
    private var activePointerId = -1

            field = value
        }
    var range: Range? = null
        set(value) {
    private var activePointerId = -1

            field = value
        }
    var orientation: Byte = 0
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                boundingBoxNeedsUpdate = true
            }
        }
    var customIconId: String? = null
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                oldCustomIconId = value
                clipIcon = null
            }
        }
    private var oldCustomIconId: String? = null
    private var clipIcon: Bitmap? = null
    var backgroundColor: Int = 0
        set(value) {
    private var activePointerId = -1

            if (field != value) {
    private var activePointerId = -1

                field = value
                oldBackgroundColor = value
                clipIcon = null
            }
        }
    private var oldBackgroundColor: Int = -1
    var cheatCodeText: String = "None"
    private var cheatCodePressed = false

    private var currentPointerId: Int = -1
    private val boundingBox: Rect = Rect()
    private var boundingBoxNeedsUpdate: Boolean = true
    private val states: BooleanArray = booleanArrayOf(false, false, false, false)
    private var currentPosition: PointF? = null
    private var touchTime: Long? = null

    private var scroller: RangeScroller? = null
    private var interpolator: CubicBezierInterpolator? = null

    private fun reset() {
    private var activePointerId = -1

        text = ""
        iconId = 0
        range = null
        scroller = null
        customIconId = null
        clipIcon = null
        backgroundColor = 0
        oldBackgroundColor = -1
        boundingBoxNeedsUpdate = true
    }

    fun initDefaultBindings() {
    private var activePointerId = -1

        when (type) {
    private var activePointerId = -1

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
            else -> {}
        }
    }

    fun getBindingCount(): Int = bindings.size

    fun setBindingCount(count: Int) {
    private var activePointerId = -1

        bindings = Array(count) {
    private var activePointerId = -1
 Binding.NONE }
        states.fill(false)
        boundingBoxNeedsUpdate = true
    }

    fun getBindingAt(index: Int): Binding = if (index < bindings.size) bindings[index] else Binding.NONE

    @Suppress("UNCHECKED_CAST")
    fun setBindingAt(index: Int, binding: Binding) {
    private var activePointerId = -1

        if (index >= bindings.size) {
    private var activePointerId = -1

            val oldLength = bindings.size
            val newBindings = arrayOfNulls<Binding>(index + 1) as Array<Binding>
            for (i in bindings.indices) {
    private var activePointerId = -1

                newBindings[i] = bindings[i]
            }
            for (i in oldLength until newBindings.size) {
    private var activePointerId = -1

                newBindings[i] = Binding.NONE
            }
            bindings = newBindings
            states.fill(false)
            boundingBoxNeedsUpdate = true
        }
        bindings[index] = binding
    }

    fun setBinding(binding: Binding) {
    private var activePointerId = -1

        bindings.fill(binding)
    }

    fun getBoundingBox(): Rect {
        if (boundingBoxNeedsUpdate) computeBoundingBox()
        return boundingBox
    }

    private fun computeBoundingBox() {
    private var activePointerId = -1

        val snappingSize = inputControlsView.snappingSize
        var halfWidth = 0
        var halfHeight = 0

        when (type) {
    private var activePointerId = -1

            Type.BUTTON, Type.COMBINE_BUTTON, Type.CHEAT_CODE_TEXT -> {
                when (shape) {
    private var activePointerId = -1

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
            Type.RANGE_BUTTON -> {
                halfWidth = (bindings.size * 4 * snappingSize) / 2
                halfHeight = snappingSize * 2

                if (orientation == 1.toByte()) {
    private var activePointerId = -1

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

    fun draw(canvas: Canvas) {
    private var activePointerId = -1

        val snappingSize = inputControlsView.snappingSize
        val paint = inputControlsView.getPaint()
        val primaryColor = inputControlsView.getPrimaryColor()

        paint.color = if (isSelected) inputControlsView.getSecondaryColor() else primaryColor
        paint.style = Paint.Style.STROKE
        val strokeWidth = snappingSize * 0.25f
        paint.strokeWidth = strokeWidth
        val box = getBoundingBox()

        when (type) {
    private var activePointerId = -1

            Type.BUTTON, Type.COMBINE_BUTTON, Type.CHEAT_CODE_TEXT -> drawButton(canvas, paint, box, primaryColor, strokeWidth)
            Type.D_PAD -> drawDPad(canvas, paint, box)
            Type.RANGE_BUTTON -> drawRangeButton(canvas, paint, box, strokeWidth)
            Type.STICK -> drawStick(canvas, paint, box, primaryColor, strokeWidth)
            Type.TRACKPAD -> drawTrackpad(canvas, paint, box, strokeWidth)
        }
    }

    private fun drawButton(canvas: Canvas, paint: Paint, box: Rect, primaryColor: Int, strokeWidth: Float) {
    private var activePointerId = -1

        val cx = box.centerX().toFloat()
        val cy = box.centerY().toFloat()

        when (shape) {
    private var activePointerId = -1

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
    private var activePointerId = -1

            drawCustomIcon(canvas, cx, cy, box.width().toFloat(), box.height().toFloat())
        } else if (backgroundColor > 0) {
    private var activePointerId = -1

            drawColorSolidIcon(canvas, cx, cy, box.width().toFloat(), box.height().toFloat())
        } else if (iconId > 0) {
    private var activePointerId = -1

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
    private var activePointerId = -1

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
    private var activePointerId = -1

        val paint = inputControlsView.getPaint()
        val iconId = customIconId ?: return
        
        var icon: Bitmap? = if (clipIcon != null && oldCustomIconId == iconId) {
    private var activePointerId = -1

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
    private var activePointerId = -1

        val paint = inputControlsView.getPaint()
        val color = backgroundColor
        
        var icon: Bitmap? = if (clipIcon != null && oldBackgroundColor == color) {
    private var activePointerId = -1

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
    private var activePointerId = -1

        val cx = box.centerX().toFloat()
        val cy = box.centerY().toFloat()
        val snappingSize = inputControlsView.snappingSize
        val offsetX = snappingSize * 2 * scale
        val offsetY = snappingSize * 3 * scale
        val start = snappingSize * scale

        val path = inputControlsView.getPath()
        path.reset()

        path.moveTo(cx, cy - start)
        path.lineTo(cx - offsetX, cy - offsetY)
        path.lineTo(cx - offsetX, box.top.toFloat())
        path.lineTo(cx + offsetX, box.top.toFloat())
        path.lineTo(cx + offsetX, cy - offsetY)
        path.close()

        path.moveTo(cx - start, cy)
        path.lineTo(cx - offsetY, cy - offsetX)
        path.lineTo(box.left.toFloat(), cy - offsetX)
        path.lineTo(box.left.toFloat(), cy + offsetX)
        path.lineTo(cx - offsetY, cy + offsetX)
        path.close()

        path.moveTo(cx, cy + start)
        path.lineTo(cx - offsetX, cy + offsetY)
        path.lineTo(cx - offsetX, box.bottom.toFloat())
        path.lineTo(cx + offsetX, box.bottom.toFloat())
        path.lineTo(cx + offsetX, cy + offsetY)
        path.close()

        path.moveTo(cx + start, cy)
        path.lineTo(cx + offsetY, cy - offsetX)
        path.lineTo(box.right.toFloat(), cy - offsetX)
        path.lineTo(box.right.toFloat(), cy + offsetX)
        path.lineTo(cx + offsetY, cy + offsetX)
        path.close()

        canvas.drawPath(path, paint)

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
    private var activePointerId = -1

        val snappingSize = inputControlsView.snappingSize
        val radius = snappingSize * 0.75f * scale

        if (orientation == 0.toByte()) {
    private var activePointerId = -1

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
    private var activePointerId = -1

                val index = i % currentRange.max.toInt()

                paint.style = Paint.Style.STROKE
                paint.color = paint.color

                if (startX > box.left && startX < box.right) {
    private var activePointerId = -1

                    canvas.drawLine(startX, lineTop, startX, lineBottom, paint)
                }

                val text = getRangeTextForIndex(currentRange, index)
                if (startX < box.right && startX + elementSize > box.left) {
    private var activePointerId = -1

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
    private var activePointerId = -1

                val index = i % currentRange.max.toInt()

                paint.style = Paint.Style.STROKE
                paint.color = paint.color

                if (startY > box.top && startY < box.bottom) {
    private var activePointerId = -1

                    canvas.drawLine(lineLeft, startY, lineRight, startY, paint)
                }

                val text = getRangeTextForIndex(currentRange, index)
                if (startY < box.bottom && startY + elementSize > box.top) {
    private var activePointerId = -1

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
    private var activePointerId = -1

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
    private var activePointerId = -1

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
    private var activePointerId = -1

            return text
        }

        val binding = getBindingAt(0)
        var displayText = binding.toString()
            .replace("NUMPAD ", "NP")
            .replace("BUTTON ", "")

        if (displayText.length > 7) {
    private var activePointerId = -1

            val parts = displayText.split(" ")
            val sb = StringBuilder()
            for (part in parts) {
    private var activePointerId = -1

                if (part.isNotEmpty()) {
    private var activePointerId = -1

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
    private var activePointerId = -1

            Range.FROM_A_TO_Z -> ('A'.code + index).toChar().toString()
            Range.FROM_0_TO_9 -> ((index + 1) % 10).toString()
            Range.FROM_F1_TO_F12 -> "F${index + 1}"
            Range.FROM_NP0_TO_NP9 -> "NP${(index + 1) % 10}"
        }
    }

    fun handleTouchDown(pointerId: Int, px: Float, py: Float): Boolean {
        if (currentPointerId == -1 && containsPoint(px, py)) {
    private var activePointerId = -1

            currentPointerId = pointerId

            when (type) {
    private var activePointerId = -1

                Type.CHEAT_CODE_TEXT -> {
                    if (!cheatCodePressed) {
    private var activePointerId = -1

                        for (c in cheatCodeText) {
    private var activePointerId = -1

                            inputControlsView.handleInputEvent(Binding.NONE, true)
                        }
                        cheatCodePressed = true
                    }
                    return true
                }
                Type.COMBINE_BUTTON -> {
                    if (isKeepButtonPressedAfterMinTime()) {
    private var activePointerId = -1

                        touchTime = System.currentTimeMillis()
                    }
                    if (!isToggleSwitch || !isSelected) {
    private var activePointerId = -1

                        for (i in states.indices) {
    private var activePointerId = -1

                            if (getBindingAt(i) != Binding.NONE) {
    private var activePointerId = -1

                                inputControlsView.handleInputEvent(getBindingAt(i), true)
                            }
                        }
                    }
                    return true
                }
                Type.BUTTON -> {
                    if (isKeepButtonPressedAfterMinTime()) {
    private var activePointerId = -1

                        touchTime = System.currentTimeMillis()
                    }
                    if (!isToggleSwitch || !isSelected) {
    private var activePointerId = -1

                        val binding = getBindingAt(0)
                        // winlator逻辑：只发送一次keyDown，让X11自动处理repeat
                        inputControlsView.handleInputEvent(binding, true)
                    }
                    return true
                }
                Type.RANGE_BUTTON -> {
                    if (scroller == null) {
    private var activePointerId = -1

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
                    // 简化逻辑：只在handleTouchMove中处理，不在handleTouchDown中发送初始事件
                    // 这避免了多次发送keyDown导致的问题
                    if (currentPosition == null) currentPosition = PointF()
                    currentPosition?.set(px, py)
                    return handleTouchMove(pointerId, px, py)
                }
            }
        }
        return false
    }



    fun handleTouchMove(pointerId: Int, px: Float, py: Float): Boolean {
        if (pointerId == currentPointerId) {
    private var activePointerId = -1

            when (type) {
    private var activePointerId = -1

                Type.BUTTON -> {
                    // winlator逻辑：BUTTON类型不需要在MOVE时做任何事情
                    // 持续按下状态由X11服务端自动处理
                    return true
                }
                Type.D_PAD, Type.STICK, Type.TRACKPAD -> {
                    var deltaX: Float
                    var deltaY: Float
                    val box = getBoundingBox()
                    val radius = box.width() * 0.5f

                    when (type) {
    private var activePointerId = -1

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
    private var activePointerId = -1

                                val angle = atan2(offsetY, offsetX)
                                offsetX = (cos(angle) * radius).toFloat()
                                offsetY = (sin(angle) * radius).toFloat()
                            }

                            deltaX = clamp(offsetX / radius, -1f, 1f)
                            deltaY = clamp(offsetY / radius, -1f, 1f)
                        }
                    }

                    when (type) {
    private var activePointerId = -1

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
    private var activePointerId = -1

                                val value = if (i == 1 || i == 3) deltaX else deltaY
                                val binding = getBindingAt(i)
                                if (binding == Binding.NONE) continue

                                if (binding.isGamepad) {
    private var activePointerId = -1

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
    private var activePointerId = -1

                                val value = if (i == 1 || i == 3) deltaX else deltaY
                                val binding = getBindingAt(i)

                                if (binding.isGamepad) {
    private var activePointerId = -1

                                    if (abs(value) > TRACKPAD_ACCELERATION_THRESHOLD) {
    private var activePointerId = -1

                                        inputControlsView.handleInputEvent(binding, true, value * STICK_SENSITIVITY)
                                    }
                                    states[i] = true
                                } else {
                                    if (abs(value) > 4) {
    private var activePointerId = -1

                                        when (binding) {
    private var activePointerId = -1

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
    private var activePointerId = -1

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

                            // 总是更新states并发送事件
                            for (i in 0..3) {
    private var activePointerId = -1

                                val binding = getBindingAt(i)
                                if (binding == Binding.NONE) continue
                                
                                val value = if (i == 1 || i == 3) deltaX else deltaY
                                val state = if (binding.isMouseMove()) (newStates[i] || newStates[(i + 2) % 4]) else newStates[i]
                                
                                if (state) {
    private var activePointerId = -1

                                    inputControlsView.handleInputEvent(binding, true, value)
                                } else if (states[i]) {
    private var activePointerId = -1

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
                    // COMBINE_BUTTON, CHEAT_CODE_TEXT 不需要处理移动
                    return false
                }
            }
        }
        return false
    }

    fun ownsPointer(pointerId: Int): Boolean = currentPointerId == pointerId

    fun handleTouchUp(pointerId: Int): Boolean {
        if (pointerId == currentPointerId) {
    private var activePointerId = -1

            when (type) {
    private var activePointerId = -1

                Type.CHEAT_CODE_TEXT -> {
                    cheatCodePressed = false
                }
                Type.COMBINE_BUTTON -> {
                    if (isKeepButtonPressedAfterMinTime() && touchTime != null) {
    private var activePointerId = -1

                        isSelected = (System.currentTimeMillis() - touchTime!!) > BUTTON_MIN_TIME_TO_KEEP_PRESSED
                        if (!isSelected) {
    private var activePointerId = -1

                            for (i in states.indices.reversed()) {
    private var activePointerId = -1

                                if (getBindingAt(i) != Binding.NONE) {
    private var activePointerId = -1

                                    inputControlsView.handleInputEvent(getBindingAt(i), false)
                                }
                            }
                        }
                        touchTime = null
                        inputControlsView.invalidate()
                    } else if (!isToggleSwitch || isSelected) {
    private var activePointerId = -1

                        for (i in states.indices.reversed()) {
    private var activePointerId = -1

                            if (getBindingAt(i) != Binding.NONE) {
    private var activePointerId = -1

                                inputControlsView.handleInputEvent(getBindingAt(i), false)
                            }
                        }
                    }

                    if (isToggleSwitch) {
    private var activePointerId = -1

                        isSelected = !isSelected
                        inputControlsView.invalidate()
                    }
                }
                Type.BUTTON -> {
                    // winlator逻辑：只发送keyUp，让X11停止repeat
                    val binding = getBindingAt(0)
                    inputControlsView.handleInputEvent(binding, false)

                    if (isToggleSwitch) {
    private var activePointerId = -1

                        isSelected = !isSelected
                        inputControlsView.invalidate()
                    }
                }
                Type.RANGE_BUTTON, Type.D_PAD, Type.STICK, Type.TRACKPAD -> {
                    for (i in states.indices) {
    private var activePointerId = -1

                        if (states[i]) inputControlsView.handleInputEvent(getBindingAt(i), false)
                        states[i] = false
                    }

                    if (type == Type.RANGE_BUTTON) {
    private var activePointerId = -1

                        scroller?.handleTouchUp()
                    } else if (type == Type.STICK) {
    private var activePointerId = -1

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

    private fun isKeepButtonPressedAfterMinTime(): Boolean {
        val binding = getBindingAt(0)
        return !isToggleSwitch && (binding == Binding.GAMEPAD_BUTTON_THUMBL || binding == Binding.GAMEPAD_BUTTON_THUMBR)
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return maxOf(min, minOf(max, value))
    }

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
    private var activePointerId = -1

            json.put("cheatCodeText", cheatCodeText)
        }
        
        if (!customIconId.isNullOrEmpty()) {
    private var activePointerId = -1

            json.put("customIconId", customIconId)
        }
        
        if (backgroundColor > 0) {
    private var activePointerId = -1

            json.put("backgroundColor", backgroundColor)
        }

        if (type == Type.RANGE_BUTTON && range != null) {
    private var activePointerId = -1

            json.put("range", range!!.name)
            if (orientation != 0.toByte()) json.put("orientation", orientation.toInt())
        }

        return json
    }
    private fun forceReleaseAllKeys() {
        inputControlsView.inputEventSender?.let {
            // fallback release all common keys
        }
    }
}

class CubicBezierInterpolator {
    private var mX1 = 0f
    private var mY1 = 0f
    private var mX2 = 0f
    private var mY2 = 0f
    private var mSamples = 200
    private var mCurve = FloatArray(0)

    fun set(x1: Float, y1: Float, x2: Float, y2: Float) {
    private var activePointerId = -1

        if (x1 != mX1 || y1 != mY1 || x2 != mX2 || y2 != mY2) {
    private var activePointerId = -1

            mX1 = x1
            mY1 = y1
            mX2 = x2
            mY2 = y2
            mCurve = FloatArray(mSamples)
            for (i in 0 until mSamples) {
    private var activePointerId = -1

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
    private fun forceReleaseAllKeys() {
        inputControlsView.inputEventSender?.let {
            // fallback release all common keys
        }
    }
}