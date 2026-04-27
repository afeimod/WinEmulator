package org.github.ewt45.winemulator.inputcontrols

import org.github.ewt45.winemulator.inputcontrols.ControlElement.Range

/**
 * Handles scrolling for range button elements
 * 参考 winlator 的实现，重写滚动逻辑
 */
class RangeScroller(
    private val inputControlsView: InputControlsView,
    private val element: ControlElement
) {
    companion object {
        // 参考 winlator TouchpadView 的常量定义
        private const val MAX_TAP_MILLISECONDS: Long = 200
        private const val MAX_TAP_TRAVEL_DISTANCE: Float = 10f
    }

    private var scrollOffset: Float = 0f
    private var currentOffset: Float = 0f
    private var lastPosition: Float = 0f
    private var touchTime: Long = 0
    private var binding: Binding = Binding.NONE
    private var isActionDown: Boolean = false
    private var isScrolling: Boolean = false

    fun getElementSize(): Float {
        val boundingBox = element.getBoundingBox()
        val range = element.range ?: Range.FROM_A_TO_Z
        return maxOf(boundingBox.width().toFloat(), boundingBox.height().toFloat()) / range.max.toFloat()
    }

    fun getScrollSize(): Float {
        val range = element.range ?: Range.FROM_A_TO_Z
        return getElementSize() * range.max.toFloat()
    }

    fun getScrollOffset(): Float = scrollOffset

    fun setScrollOffset(offset: Float) {
        scrollOffset = offset
    }

    fun getRangeIndex(): IntArray {
        val range = element.range ?: Range.FROM_A_TO_Z
        val from = ((scrollOffset / getElementSize()) % range.max).toInt()
        val adjustedFrom = if (from < 0) range.max + from else from
        val to = adjustedFrom + element.getBindingCount() + 1
        return intArrayOf(adjustedFrom, to)
    }

    /**
     * 根据触摸位置获取对应的 Binding
     * 参考 winlator 的 getBindingByPosition 实现
     */
    private fun getBindingByPosition(x: Float, y: Float): Binding {
        val boundingBox = element.getBoundingBox()
        val range = element.range ?: Range.FROM_A_TO_Z
        val orientation = element.orientation.toInt()

        val offset = if (orientation == 0) {
            x - boundingBox.left - currentOffset
        } else {
            y - boundingBox.top - currentOffset
        }

        val index = ((offset / getElementSize()) % range.max).toInt()
        val adjustedIndex = if (index < 0) range.max + index else index

        return when (range) {
            Range.FROM_A_TO_Z -> {
                val charIndex = 65 + adjustedIndex
                if (charIndex in 65..90) {
                    Binding.fromString("KEY_${Char(charIndex)}")
                } else {
                    Binding.NONE
                }
            }
            Range.DIGITS -> {
                val numIndex = (adjustedIndex + 1) % 10
                Binding.fromString("KEY_$numIndex")
            }
            Range.FUNCTION_KEYS -> {
                Binding.fromString("KEY_F${adjustedIndex + 1}")
            }
            Range.NUMPAD_DIGITS -> {
                val numpadIndex = (adjustedIndex + 1) % 10
                Binding.fromString("KEY_KP_$numpadIndex")
            }
        }
    }

    /**
     * 判断是否是点击（而非拖拽）
     * 参考 winlator 的 isTap 实现
     */
    private fun isTap(): Boolean {
        return System.currentTimeMillis() - touchTime < MAX_TAP_MILLISECONDS
    }

    /**
     * 处理触摸按下事件
     * @param element 当前元素（用于兼容性）
     * @param x 触摸 X 坐标
     * @param y 触摸 Y 坐标
     */
    fun handleTouchDown(element: ControlElement, x: Float, y: Float) {
        isActionDown = true
        isScrolling = false
        binding = getBindingByPosition(x, y)
        touchTime = System.currentTimeMillis()
        lastPosition = if (element.orientation.toInt() == 0) x else y
        currentOffset = 0f
        element.setBinding(Binding.NONE)
    }

    /**
     * 处理触摸移动事件
     * @param element 当前元素（用于兼容性）
     * @param x 触摸 X 坐标
     * @param y 触摸 Y 坐标
     */
    fun handleTouchMove(element: ControlElement, x: Float, y: Float) {
        if (!isActionDown) return

        val position = if (element.orientation.toInt() == 0) x else y
        val deltaPosition = position - lastPosition

        // 如果移动距离超过阈值，切换到滚动模式
        if (Math.abs(deltaPosition) >= MAX_TAP_TRAVEL_DISTANCE) {
            isScrolling = true
        }

        if (isScrolling) {
            currentOffset += deltaPosition

            val scrollSize = getScrollSize()
            scrollOffset = -currentOffset % scrollSize
            if (scrollOffset < 0) {
                scrollOffset = scrollSize + scrollOffset
            }

            lastPosition = position
        }
    }

    fun handleTouchUp() {
        if (isActionDown) {
            if (isTap() && !isScrolling) {
                // 点击：发送按下和释放事件
                inputControlsView.handleInputEvent(binding, true)
                val finalBinding = binding
                inputControlsView.postDelayed({
                    inputControlsView.handleInputEvent(finalBinding, false)
                }, 30)
            } else {
                // 滚动释放：只发送释放事件
                inputControlsView.handleInputEvent(binding, false)
            }
        }
        isActionDown = false
    }

    fun isScrolling(): Boolean = isScrolling
}
