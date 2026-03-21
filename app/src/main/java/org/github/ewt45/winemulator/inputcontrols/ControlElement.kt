package org.github.ewt45.winemulator.inputcontrols

import android.graphics.*
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Range
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Shape
import org.github.ewt45.winemulator.inputcontrols.ControlElement.Type

/**
 * Represents a single control element (button, d-pad, stick, etc.)
 */
class ControlElement(
    private val inputControlsView: InputControlsView
) {
    companion object {
        const val STICK_DEAD_ZONE = 0.15f
        const val DPAD_DEAD_ZONE = 0.3f
        const val STICK_SENSITIVITY = 3.0f
        const val TRACKPAD_MIN_SPEED = 0.8f
        const val TRACKPAD_MAX_SPEED = 20.0f
        const val TRACKPAD_ACCELERATION_THRESHOLD: Byte = 4
        const val BUTTON_MIN_TIME_TO_KEEP_PRESSED: Short = 300
    }

    enum class Type {
        BUTTON,
        D_PAD,
        RANGE_BUTTON,
        STICK,
        TRACKPAD;

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
        FROM_A_TO_Z(26),
        FROM_0_TO_9(10),
        FROM_F1_TO_F12(12),
        FROM_NP0_TO_NP9(10);

        companion object {
            fun names(): Array<String> = entries.map { it.name.replace("_", " ") }.toTypedArray()
        }
    }

    var type: Type = Type.BUTTON
    var shape: Shape = Shape.CIRCLE
    private var bindings: Array<Binding> = arrayOf(Binding.NONE, Binding.NONE, Binding.NONE, Binding.NONE)
    var scale: Float = 1.0f
    var x: Int = 0
    var y: Int = 0
    var isSelected: Boolean = false
    var isToggleSwitch: Boolean = false
    var text: String = ""
    var iconId: Byte = 0
    var range: Range? = null
    var orientation: Byte = 0 // 0 = horizontal, 1 = vertical

    private var currentPointerId: Int = -1
    private val boundingBox: Rect = Rect()
    private var boundingBoxNeedsUpdate: Boolean = true
    private val states: BooleanArray = booleanArrayOf(false, false, false, false)
    private var currentPosition: PointF? = null
    private var touchTime: Long? = null

    private fun reset() {
        setBinding(Binding.NONE)

        when (type) {
            Type.D_PAD, Type.STICK -> {
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
                // Range buttons have their own scroller
            }
            else -> {}
        }

        text = ""
        iconId = 0
        range = null
        boundingBoxNeedsUpdate = true
    }

    fun setType(type: Type) {
        this.type = type
        reset()
    }

    fun getBindingCount(): Int = bindings.size

    fun setBindingCount(count: Int) {
        bindings = Array(count) { Binding.NONE }
        states.fill(false)
        boundingBoxNeedsUpdate = true
    }

    fun getBindingAt(index: Int): Binding = if (index < bindings.size) bindings[index] else Binding.NONE

    fun setBindingAt(index: Int, binding: Binding) {
        if (index >= bindings.size) {
            val oldLength = bindings.size
            @Suppress("UNCHECKED_CAST")
            bindings = bindings.copyOf(index + 1) as Array<Binding>
            for (i in oldLength until bindings.size) {
                bindings[i] = Binding.NONE
            }
            states.fill(false)
            boundingBoxNeedsUpdate = true
        }
        bindings[index] = binding
    }

    fun setBinding(binding: Binding) {
        bindings.fill(binding)
    }

    fun getBoundingBox(): Rect {
        if (boundingBoxNeedsUpdate) computeBoundingBox()
        return boundingBox
    }

    private fun computeBoundingBox() {
        val snappingSize = inputControlsView.snappingSize
        var halfWidth = 0
        var halfHeight = 0

        when (type) {
            Type.BUTTON -> {
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

    fun handleTouchDown(pointerId: Int, px: Float, py: Float): Boolean {
        if (currentPointerId == -1 && containsPoint(px, py)) {
            currentPointerId = pointerId

            when (type) {
                Type.BUTTON -> {
                    if (isKeepButtonPressedAfterMinTime()) {
                        touchTime = System.currentTimeMillis()
                    }
                    if (!isToggleSwitch || !isSelected) {
                        inputControlsView.handleInputEvent(getBindingAt(0), true)
                        inputControlsView.handleInputEvent(getBindingAt(1), true)
                    }
                    return true
                }
                Type.RANGE_BUTTON -> {
                    inputControlsView.getRangeScroller()?.handleTouchDown(this, px, py)
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

    fun handleTouchMove(pointerId: Int, px: Float, py: Float): Boolean {
        if (pointerId == currentPointerId && (type == Type.D_PAD || type == Type.STICK || type == Type.TRACKPAD)) {
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

                    val distance = kotlin.math.sqrt((radius - localX) * (radius - localX) + (radius - localY) * (radius - localY))
                    if (distance > radius) {
                        val angle = kotlin.math.atan2(offsetY, offsetX)
                        offsetX = (kotlin.math.cos(angle) * radius).toFloat()
                        offsetY = (kotlin.math.sin(angle) * radius).toFloat()
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

                        if (binding.isGamepad()) {
                            val adjustedValue = clamp(
                                maxOf(0f, kotlin.math.abs(value) - 0.01f) * kotlin.math.sign(value) * STICK_SENSITIVITY,
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

                        if (binding.isGamepad()) {
                            if (kotlin.math.abs(value) > TRACKPAD_ACCELERATION_THRESHOLD) {
                                inputControlsView.handleInputEvent(binding, true, value * STICK_SENSITIVITY)
                            }
                            states[i] = true
                        } else {
                            if (kotlin.math.abs(value) > 4) {
                                when (binding) {
                                    Binding.MOUSE_MOVE_LEFT, Binding.MOUSE_MOVE_RIGHT -> cursorDx = kotlin.math.round(value).toInt()
                                    Binding.MOUSE_MOVE_UP, Binding.MOUSE_MOVE_DOWN -> cursorDy = kotlin.math.round(value).toInt()
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
                        inputControlsView.handleInputEvent(binding, state, value)
                        states[i] = state
                    }
                }
                else -> {}
            }
            return true
        } else if (pointerId == currentPointerId && type == Type.RANGE_BUTTON) {
            inputControlsView.getRangeScroller()?.handleTouchMove(this, px, py)
            return true
        }
        return false
    }

    fun handleTouchUp(pointerId: Int): Boolean {
        if (pointerId == currentPointerId) {
            when (type) {
                Type.BUTTON -> {
                    if (isKeepButtonPressedAfterMinTime() && touchTime != null) {
                        isSelected = (System.currentTimeMillis() - touchTime!!) > BUTTON_MIN_TIME_TO_KEEP_PRESSED
                        if (!isSelected) {
                            inputControlsView.handleInputEvent(getBindingAt(0), false)
                            inputControlsView.handleInputEvent(getBindingAt(1), false)
                        }
                        touchTime = null
                        inputControlsView.invalidate()
                    } else if (!isToggleSwitch || isSelected) {
                        inputControlsView.handleInputEvent(getBindingAt(0), false)
                        inputControlsView.handleInputEvent(getBindingAt(1), false)
                    }

                    if (isToggleSwitch) {
                        isSelected = !isSelected
                        inputControlsView.invalidate()
                    }
                }
                Type.RANGE_BUTTON, Type.D_PAD, Type.STICK, Type.TRACKPAD -> {
                    for (i in states.indices) {
                        if (states[i]) inputControlsView.handleInputEvent(getBindingAt(i), false)
                        states[i] = false
                    }

                    if (type == Type.RANGE_BUTTON) {
                        inputControlsView.getRangeScroller()?.handleTouchUp()
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

        if (type == Type.RANGE_BUTTON && range != null) {
            json.put("range", range!!.name)
            if (orientation != 0.toByte()) json.put("orientation", orientation.toInt())
        }

        return json
    }
}
