package org.github.ewt45.winemulator.inputcontrols

import org.github.ewt45.winemulator.inputcontrols.ControlElement.Range

/**
 * Handles scrolling for range button elements
 * 修复版：改进滚动逻辑，支持正确的拖拽滚动
 */
class RangeScroller(
    private val inputControlsView: InputControlsView,
    private val element: ControlElement
) {
    private var scrollOffset: Float = 0f
    private val rangeIndex = intArrayOf(0, 26) // Start and end indices
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var isDragging = false
    private var lastActivatedIndex: Int = -1

    // 记录开始拖拽时的初始位置，避免累积误差
    private var dragStartOffset: Float = 0f
    private var dragStartX: Float = 0f
    private var dragStartY: Float = 0f

    fun getElementSize(): Float {
        return inputControlsView.snappingSize * 4f * element.scale
    }

    fun getScrollOffset(): Float = scrollOffset

    fun setScrollOffset(offset: Float) {
        scrollOffset = offset
    }

    fun getRangeIndex(): IntArray = rangeIndex

    fun handleTouchDown(element: ControlElement, x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
        dragStartX = x
        dragStartY = y
        dragStartOffset = scrollOffset
        isDragging = true
        lastActivatedIndex = -1
        updateRangeIndex(element, x, y)
    }

    fun handleTouchMove(element: ControlElement, x: Float, y: Float) {
        if (!isDragging) return

        val delta: Float = if (element.orientation == 0.toByte()) {
            x - lastTouchX
        } else {
            y - lastTouchY
        }

        // 累加滚动偏移量
        scrollOffset -= delta
        lastTouchX = x
        lastTouchY = y

        // 根据当前滚动位置计算可见范围
        updateVisibleRange(element)
        
        // 触发当前索引对应的按键
        val currentCenter = calculateCenterIndex(element)
        triggerBindingForIndex(element, currentCenter)
        
        inputControlsView.invalidate()
    }

    fun handleTouchUp() {
        // Release any held binding
        if (lastActivatedIndex >= 0) {
            val binding = getBindingForRangeIndex(element.range, lastActivatedIndex)
            if (binding != Binding.NONE) {
                inputControlsView.handleInputEvent(binding, false)
            }
            lastActivatedIndex = -1
        }
        isDragging = false
    }

    /**
     * 根据范围类型和索引获取对应的Binding
     * Converts a range index to the corresponding Binding
     */
    private fun getBindingForRangeIndex(range: Range?, index: Int): Binding {
        val currentRange = range ?: Range.FROM_A_TO_Z
        return when (currentRange) {
            Range.FROM_A_TO_Z -> {
                // A-Z: index 0-25 对应 Binding.KEY_A 到 Binding.KEY_Z
                when (index) {
                    0 -> Binding.KEY_A
                    1 -> Binding.KEY_B
                    2 -> Binding.KEY_C
                    3 -> Binding.KEY_D
                    4 -> Binding.KEY_E
                    5 -> Binding.KEY_F
                    6 -> Binding.KEY_G
                    7 -> Binding.KEY_H
                    8 -> Binding.KEY_I
                    9 -> Binding.KEY_J
                    10 -> Binding.KEY_K
                    11 -> Binding.KEY_L
                    12 -> Binding.KEY_M
                    13 -> Binding.KEY_N
                    14 -> Binding.KEY_O
                    15 -> Binding.KEY_P
                    16 -> Binding.KEY_Q
                    17 -> Binding.KEY_R
                    18 -> Binding.KEY_S
                    19 -> Binding.KEY_T
                    20 -> Binding.KEY_U
                    21 -> Binding.KEY_V
                    22 -> Binding.KEY_W
                    23 -> Binding.KEY_X
                    24 -> Binding.KEY_Y
                    25 -> Binding.KEY_Z
                    else -> Binding.NONE
                }
            }
            Range.DIGITS -> {
                // 0-9: index 0-9 对应 Binding.KEY_0 到 Binding.KEY_9
                when (index) {
                    0 -> Binding.KEY_0
                    1 -> Binding.KEY_1
                    2 -> Binding.KEY_2
                    3 -> Binding.KEY_3
                    4 -> Binding.KEY_4
                    5 -> Binding.KEY_5
                    6 -> Binding.KEY_6
                    7 -> Binding.KEY_7
                    8 -> Binding.KEY_8
                    9 -> Binding.KEY_9
                    else -> Binding.NONE
                }
            }
            Range.FUNCTION_KEYS -> {
                // F1-F12: index 0-11 对应 Binding.KEY_F1 到 Binding.KEY_F12
                when (index) {
                    0 -> Binding.KEY_F1
                    1 -> Binding.KEY_F2
                    2 -> Binding.KEY_F3
                    3 -> Binding.KEY_F4
                    4 -> Binding.KEY_F5
                    5 -> Binding.KEY_F6
                    6 -> Binding.KEY_F7
                    7 -> Binding.KEY_F8
                    8 -> Binding.KEY_F9
                    9 -> Binding.KEY_F10
                    10 -> Binding.KEY_F11
                    11 -> Binding.KEY_F12
                    else -> Binding.NONE
                }
            }
            Range.NUMPAD_DIGITS -> {
                // NP0-NP9: index 0-9 对应 Binding.NUMPAD_0 到 Binding.NUMPAD_9
                when (index) {
                    0 -> Binding.NUMPAD_0
                    1 -> Binding.NUMPAD_1
                    2 -> Binding.NUMPAD_2
                    3 -> Binding.NUMPAD_3
                    4 -> Binding.NUMPAD_4
                    5 -> Binding.NUMPAD_5
                    6 -> Binding.NUMPAD_6
                    7 -> Binding.NUMPAD_7
                    8 -> Binding.NUMPAD_8
                    9 -> Binding.NUMPAD_9
                    else -> Binding.NONE
                }
            }
        }
    }

    /**
     * 根据触摸位置计算当前的中心索引
     */
    private fun calculateCenterIndex(element: ControlElement): Int {
        val range = element.range ?: Range.FROM_A_TO_Z
        val elementSize = getElementSize()
        val box = element.getBoundingBox()
        
        // 计算触摸点在元素内的相对位置（0到1之间）
        val position: Float = if (element.orientation == 0.toByte()) {
            // 水平方向
            val relativeX = (lastTouchX - box.left + scrollOffset) / (box.width())
            relativeX.coerceIn(0f, 1f)
        } else {
            // 垂直方向
            val relativeY = (lastTouchY - box.top + scrollOffset) / (box.height())
            relativeY.coerceIn(0f, 1f)
        }
        
        // 根据相对位置计算索引
        val maxIndex = range.max.toInt() - 1
        return (position * maxIndex).toInt().coerceIn(0, maxIndex)
    }

    /**
     * 更新可见范围
     */
    private fun updateVisibleRange(element: ControlElement) {
        val range = element.range ?: Range.FROM_A_TO_Z
        val box = element.getBoundingBox()
        val elementSize = getElementSize()
        
        // 计算可见的元素数量
        val visibleCount = if (element.orientation == 0.toByte()) {
            (box.width() / elementSize).toInt() + 2
        } else {
            (box.height() / elementSize).toInt() + 2
        }
        
        // 根据滚动位置计算当前中心索引
        val centerIndex = calculateCenterIndex(element)
        
        // 计算可见范围的起始和结束索引
        val halfVisible = (visibleCount / 2).coerceAtLeast(1)
        val startIndex = (centerIndex - halfVisible).coerceAtLeast(0)
        val endIndex = minOf(startIndex + visibleCount, range.max.toInt())
        
        rangeIndex[0] = startIndex
        rangeIndex[1] = endIndex
    }

    /**
     * 触发指定索引的绑定
     */
    private fun triggerBindingForIndex(element: ControlElement, index: Int) {
        val range = element.range ?: Range.FROM_A_TO_Z
        
        if (index != lastActivatedIndex) {
            // 释放之前的绑定
            if (lastActivatedIndex >= 0) {
                val prevBinding = getBindingForRangeIndex(range, lastActivatedIndex)
                if (prevBinding != Binding.NONE) {
                    inputControlsView.handleInputEvent(prevBinding, false)
                }
            }

            // 激活新的绑定
            val binding = getBindingForRangeIndex(range, index)
            if (binding != Binding.NONE) {
                inputControlsView.handleInputEvent(binding, true)
                lastActivatedIndex = index
            }
        }
    }

    /**
     * 根据触摸位置更新范围索引（兼容旧接口）
     */
    private fun updateRangeIndex(element: ControlElement, x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
        updateVisibleRange(element)
        
        val currentCenter = calculateCenterIndex(element)
        triggerBindingForIndex(element, currentCenter)
    }
}
