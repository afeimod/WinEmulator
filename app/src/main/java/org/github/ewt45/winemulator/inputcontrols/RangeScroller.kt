package org.github.ewt45.winemulator.inputcontrols

import org.github.ewt45.winemulator.inputcontrols.ControlElement.Range

/**
 * Handles scrolling for range button elements
 * 完全参考 winlator 实现，确保滚动和按键输出正确
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
    private var currentBinding: Binding = Binding.NONE  // 当前滑动选中的按键
    private var isActionDown: Boolean = false
    private var isScrolling: Boolean = false

    companion object {
        // 参考 winlator TouchpadView 的常量定义
        private const val MAX_TAP_MILLISECONDS: Long = 200
        private const val MAX_TAP_TRAVEL_DISTANCE: Float = 10f
    }

    /**
     * 获取单个元素的大小
     * 完全参考 winlator 实现：基于 boundingBox 大小除以 bindingCount
     * 这确保了触摸逻辑和绘制逻辑使用相同的 elementSize
     */
    fun getElementSize(): Float {
        val boundingBox = element.getBoundingBox()
        return maxOf(boundingBox.width(), boundingBox.height()).toFloat() / element.getBindingCount()
    }

    /**
     * 获取滚动区域的总大小
     */
    fun getScrollSize(): Float {
        return getElementSize() * (element.range?.max ?: 26).toFloat()
    }

    fun getScrollOffset(): Float = scrollOffset

    /**
     * 获取当前滑动选中的按键（用于高亮显示）
     */
    fun getCurrentBinding(): Binding = currentBinding

    fun setScrollOffset(offset: Float) {
        scrollOffset = offset
    }

    /**
     * 获取可见范围索引 [from, to]
     * 完全参考 winlator 实现
     * 确保循环滚动时能显示足够的元素
     */
    fun getRangeIndex(): IntArray {
        val range = element.range ?: Range.FROM_A_TO_Z
        val elementSize = getElementSize()
        val rangeMax = range.max.toInt()

        // 基于 scrollOffset 计算起始索引（确保负数也能正确处理）
        var from = (scrollOffset / elementSize).toInt()
        // 处理负数情况
        from = ((from % rangeMax) + rangeMax) % rangeMax

        // 计算需要显示的元素数量（比 bindingCount 多几个以确保循环时能覆盖）
        val visibleCount = maxOf(element.getBindingCount() + 2, rangeMax)
        val to = (from + visibleCount) % rangeMax

        // 如果 to <= from，说明跨越了范围边界，需要两个范围
        // 但为了简化，我们直接返回 [from, from + visibleCount]，绘制时会用取模
        return intArrayOf(from, from + visibleCount)
    }

    /**
     * 根据触摸位置获取对应的 Binding
     * 完全参考 winlator 的 getBindingByPosition 实现
     */
    private fun getBindingByPosition(x: Float, y: Float): Binding {
        val boundingBox = element.getBoundingBox()
        val range = element.range ?: Range.FROM_A_TO_Z
        val orientation = element.orientation.toInt()

        // 计算相对于元素左边/上边的偏移，减去 currentOffset（拖拽偏移）
        val offset = if (orientation == 0) {
            x - boundingBox.left - currentOffset
        } else {
            y - boundingBox.top - currentOffset
        }

        // 计算元素索引
        val elementSize = getElementSize()
        var index = kotlin.math.floor((offset / elementSize) % range.max).toInt()
        if (index < 0) index = range.max + index

        // 根据范围返回对应的 Binding
        return when (range) {
            Range.FROM_A_TO_Z -> {
                if (index in 0..25) {
                    Binding.fromString("KEY_${('A'.code + index).toChar()}")
                } else {
                    Binding.NONE
                }
            }
            Range.DIGITS -> {
                // 0-9: index 对应 (index+1)%10
                if (index in 0..9) {
                    Binding.fromString("KEY_${(index + 1) % 10}")
                } else {
                    Binding.NONE
                }
            }
            Range.FUNCTION_KEYS -> {
                // F1-F12: index 0-11 对应 F1-F12
                if (index in 0..11) {
                    Binding.fromString("KEY_F${index + 1}")
                } else {
                    Binding.NONE
                }
            }
            Range.NUMPAD_DIGITS -> {
                // NP0-NP9: index 对应 (index+1)%10
                if (index in 0..9) {
                    Binding.fromString("KEY_KP_${(index + 1) % 10}")
                } else {
                    Binding.NONE
                }
            }
        }
    }

    /**
     * 判断是否是点击（而非拖拽）
     */
    private fun isTap(): Boolean {
        return System.currentTimeMillis() - touchTime < MAX_TAP_MILLISECONDS
    }

    fun handleTouchDown(element: ControlElement, x: Float, y: Float) {
        // 每次触摸都重置偏移量，从触摸位置重新开始
        scrollOffset = 0f
        currentOffset = 0f
        isScrolling = false
        isActionDown = true
        currentBinding = getBindingByPosition(x, y)  // 根据触摸位置获取当前按键
        binding = currentBinding
        touchTime = System.currentTimeMillis()
        lastPosition = if (element.orientation.toInt() == 0) x else y
        element.setBinding(Binding.NONE)
    }

    fun handleTouchMove(element: ControlElement, x: Float, y: Float) {
        if (!isActionDown) return

        val position = if (element.orientation.toInt() == 0) x else y
        val deltaPosition = position - lastPosition

        // 如果移动距离超过阈值，切换到滚动模式
        if (kotlin.math.abs(deltaPosition) >= MAX_TAP_TRAVEL_DISTANCE) {
            isScrolling = true
        }

        if (isScrolling) {
            // 累加偏移量
            currentOffset += deltaPosition

            // 计算滚动偏移（循环滚动）
            val scrollSize = getScrollSize()
            scrollOffset = -currentOffset % scrollSize
            if (scrollOffset < 0) {
                scrollOffset = scrollSize + scrollOffset
            }

            // 更新当前按下的键（基于新的偏移量）
            val newBinding = getBindingByPosition(x, y)
            if (newBinding != currentBinding) {
                // 切换按键：先释放之前的，再按下新的
                if (currentBinding != Binding.NONE) {
                    inputControlsView.handleInputEvent(currentBinding, false)
                }
                if (newBinding != Binding.NONE) {
                    inputControlsView.handleInputEvent(newBinding, true)
                }
                currentBinding = newBinding
                binding = currentBinding
            }

            lastPosition = position
        } else {
            // 未进入滚动模式时，也更新 currentBinding 以便点击时使用正确的按键
            currentBinding = getBindingByPosition(x, y)
            binding = currentBinding
        }
    }

    fun handleTouchUp() {
        if (isActionDown) {
            if (isTap() && !isScrolling) {
                // 点击：发送按下和释放事件（使用 currentBinding，因为它是根据触摸位置计算的）
                val finalBinding = currentBinding
                if (finalBinding != Binding.NONE) {
                    inputControlsView.handleInputEvent(finalBinding, true)
                    inputControlsView.postDelayed({
                        inputControlsView.handleInputEvent(finalBinding, false)
                    }, 30)
                }
            } else {
                // 滚动模式：释放当前按下的按键
                if (currentBinding != Binding.NONE) {
                    inputControlsView.handleInputEvent(currentBinding, false)
                }
            }
        }
        isActionDown = false
    }

    fun isScrolling(): Boolean = isScrolling
}