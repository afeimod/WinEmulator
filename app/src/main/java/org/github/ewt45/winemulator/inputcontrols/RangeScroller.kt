package org.github.ewt45.winemulator.inputcontrols

import org.github.ewt45.winemulator.inputcontrols.ControlElement.Range

/**
 * Handles scrolling for range button elements
 * 参考 winlator 实现修复滚动和按键输出问题
 */
class RangeScroller(
    private val inputControlsView: InputControlsView,
    private val element: ControlElement
) {
    private var scrollOffset: Float = 0f
    private var currentOffset: Float = 0f
    private var lastPosition: Float = 0f
    private var touchTime: Long = 0
    private var binding: Binding = Binding.NONE
    private var isActionDown: Boolean = false
    private var isScrolling: Boolean = false
    private val rangeIndex = intArrayOf(0, 26) // [from, to]

    companion object {
        // 参考 winlator TouchpadView 的常量定义
        private const val MAX_TAP_MILLISECONDS: Long = 200
        private const val MAX_TAP_TRAVEL_DISTANCE: Float = 10f
    }

    fun getElementSize(): Float {
        return inputControlsView.snappingSize * 4f * element.scale
    }

    fun getScrollOffset(): Float = scrollOffset

    fun setScrollOffset(offset: Float) {
        scrollOffset = offset
    }

    fun getRangeIndex(): IntArray = rangeIndex

    /**
     * 根据触摸位置获取对应的 Binding
     * 参考 winlator 的 getBindingByPosition 实现
     */
    private fun getBindingByPosition(x: Float, y: Float): Binding {
        val boundingBox = element.getBoundingBox()
        val range = element.range ?: Range.FROM_A_TO_Z
        val orientation = element.orientation.toInt()

        // 计算触摸点在元素内的相对偏移
        val offset = if (orientation == 0) {
            x - boundingBox.left - currentOffset
        } else {
            y - boundingBox.top - currentOffset
        }

        // 计算元素索引
        val elementSize = getElementSize()
        var index = ((offset / elementSize) + (scrollOffset / elementSize)).toInt()
        
        // 处理负数和越界
        index = ((index % range.max) + range.max) % range.max

        return when (range) {
            Range.FROM_A_TO_Z -> {
                // A=0, B=1, ..., Z=25
                if (index in 0..25) {
                    Binding.fromString("KEY_${('A'.code + index).toChar()}")
                } else {
                    Binding.NONE
                }
            }
            Range.DIGITS -> {
                // 0=0, 1=1, ..., 9=9
                if (index in 0..9) {
                    Binding.fromString("KEY_$index")
                } else {
                    Binding.NONE
                }
            }
            Range.FUNCTION_KEYS -> {
                // F1=1, F2=2, ..., F12=12 (索引1-12)
                val fKey = ((index % 12) + 1)
                if (fKey in 1..12) {
                    Binding.fromString("KEY_F$fKey")
                } else {
                    Binding.NONE
                }
            }
            Range.NUMPAD_DIGITS -> {
                // NP0=0, NP1=1, ..., NP9=9
                if (index in 0..9) {
                    Binding.fromString("NUMPAD_$index")
                } else {
                    Binding.NONE
                }
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

    fun handleTouchDown(element: ControlElement, x: Float, y: Float) {
        isActionDown = true
        isScrolling = false
        binding = getBindingByPosition(x, y)
        touchTime = System.currentTimeMillis()
        lastPosition = if (element.orientation.toInt() == 0) x else y
        currentOffset = 0f
        element.setBinding(Binding.NONE)
    }

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

            val scrollSize = getElementSize() * (element.range?.max ?: 26).toFloat()
            scrollOffset = -currentOffset % scrollSize
            if (scrollOffset < 0) {
                scrollOffset = scrollSize + scrollOffset
            }

            // 更新范围索引
            updateRangeIndex()

            lastPosition = position
        }
    }

    /**
     * 更新可见范围索引
     */
    private fun updateRangeIndex() {
        val range = element.range ?: Range.FROM_A_TO_Z
        val elementSize = getElementSize()
        
        // 计算基于滚动偏移的起始索引
        val scrollElementIndex = (scrollOffset / elementSize).toInt()
        var fromIndex = scrollElementIndex % range.max
        if (fromIndex < 0) fromIndex = range.max + fromIndex

        // 计算可见的元素数量（加2以便左右各显示一个额外的元素）
        val bindingCount = element.getBindingCount().coerceAtLeast(1)
        val toIndex = fromIndex + bindingCount + 1

        rangeIndex[0] = fromIndex
        rangeIndex[1] = toIndex
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
