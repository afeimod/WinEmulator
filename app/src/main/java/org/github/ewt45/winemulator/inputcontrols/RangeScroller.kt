package org.github.ewt45.winemulator.inputcontrols

import android.graphics.Rect
import java.util.Timer
import java.util.TimerTask

/**
 * Handles scrolling for range button elements
 * 完全从 termux-app 移植，确保滚动和按键输出正确
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
    private var timer: Timer? = null

    /**
     * 获取单个元素的大小
     * 基于 boundingBox 大小除以 bindingCount
     */
    fun getElementSize(): Float {
        val boundingBox = element.boundingBox
        return maxOf(boundingBox.width(), boundingBox.height()).toFloat() / element.bindingCount
    }

    /**
     * 获取滚动区域的总大小
     */
    fun getScrollSize(): Float {
        return getElementSize() * (element.range?.max ?: 26).toFloat()
    }

    fun getScrollOffset(): Float = scrollOffset

    /**
     * 获取可见范围索引 [from, to]
     */
    fun getRangeIndex(): IntArray {
        val range = element.range ?: ControlElement.Range.FROM_A_TO_Z
        val elementSize = getElementSize()
        
        // 基于 scrollOffset 计算起始索引
        var from = kotlin.math.floor((scrollOffset / elementSize) % range.max).toInt()
        if (from < 0) from = range.max.toInt() + from
        
        val to = from + element.bindingCount + 1
        
        return intArrayOf(from, to)
    }

    /**
     * 根据触摸位置获取对应的 Binding
     */
    private fun getBindingByPosition(x: Float, y: Float): Binding {
        val boundingBox = element.boundingBox
        val range = element.range ?: ControlElement.Range.FROM_A_TO_Z
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
        if (index < 0) index = range.max.toInt() + index

        // 根据范围返回对应的 Binding
        return when (range) {
            ControlElement.Range.FROM_A_TO_Z -> {
                if (index in 0..25) {
                    Binding.fromString("KEY_${('A'.code + index).toChar()}")
                } else {
                    Binding.NONE
                }
            }
            ControlElement.Range.DIGITS -> {
                if (index in 0..9) {
                    Binding.fromString("KEY_${(index + 1) % 10}")
                } else {
                    Binding.NONE
                }
            }
            ControlElement.Range.FUNCTION_KEYS -> {
                if (index in 0..11) {
                    Binding.fromString("KEY_F${index + 1}")
                } else {
                    Binding.NONE
                }
            }
            ControlElement.Range.NUMPAD_DIGITS -> {
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
        return System.currentTimeMillis() - touchTime < inputControlsView.MAX_TAP_MILLISECONDS
    }

    /**
     * 销毁计时器
     */
    private fun destroyTimer() {
        timer?.cancel()
        timer = null
    }

    /**
     * 处理触摸按下事件
     * 从 termux-app 移植
     */
    fun handleTouchDown(x: Float, y: Float) {
        destroyTimer()

        isScrolling = false
        isActionDown = true
        binding = getBindingByPosition(x, y)
        touchTime = System.currentTimeMillis()
        lastPosition = if (element.orientation.toInt() == 0) x else y
        element.setBinding(Binding.NONE)
        
        // 使用 Timer 延迟发送按键按下事件
        timer = Timer(true)
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (!isScrolling) {
                    inputControlsView.post {
                        inputControlsView.handleInputEvent(binding, true)
                    }
                }
            }
        }, inputControlsView.MAX_TAP_MILLISECONDS)
    }

    /**
     * 处理触摸移动事件
     * 从 termux-app 移植
     */
    fun handleTouchMove(x: Float, y: Float) {
        if (!isActionDown) return

        val position = if (element.orientation.toInt() == 0) x else y
        val deltaPosition = position - lastPosition

        // 如果移动距离超过阈值，切换到滚动模式
        if (kotlin.math.abs(deltaPosition) >= inputControlsView.MAX_TAP_TRAVEL_DISTANCE) {
            isScrolling = true
            destroyTimer()
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

            lastPosition = position
            // 触发视图重绘
            inputControlsView.invalidate()
        }
    }

    /**
     * 处理触摸抬起事件
     * 从 termux-app 移植
     */
    fun handleTouchUp() {
        if (isActionDown) {
            destroyTimer()
            if (isTap() && !isScrolling) {
                // 点击：发送按下事件
                inputControlsView.handleInputEvent(binding, true)
                // 延迟发送释放事件
                val finalBinding = binding
                inputControlsView.postDelayed({
                    inputControlsView.handleInputEvent(finalBinding, false)
                }, 30)
            } else {
                // 滚动：只发送释放事件
                if (binding != Binding.NONE) {
                    inputControlsView.handleInputEvent(binding, false)
                }
            }
        }
        isActionDown = false
    }

    fun isScrolling(): Boolean = isScrolling
    
    /**
     * 重置状态
     */
    fun reset() {
        destroyTimer()
        isActionDown = false
        isScrolling = false
        scrollOffset = 0f
        currentOffset = 0f
        binding = Binding.NONE
    }
}