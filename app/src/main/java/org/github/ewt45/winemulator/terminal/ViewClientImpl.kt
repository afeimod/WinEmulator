package org.github.ewt45.winemulator.terminal

import android.util.Log
import android.view.MotionEvent
import com.termux.view.TerminalView
import org.github.ewt45.winemulator.MainEmuActivity

class ViewClientImpl(
    val activity: MainEmuActivity,
    val sessionClient: SessionClientAImpl,
) : ViewClientBase() {

    private var mCurrentFontSize = 14

    /**
     * 优化后的双指缩放：
     * 1. 撑开（scale > 1）即放大，并拢（scale < 1）即缩小。
     * 2. 返回 1.0f 以重置累积缩放率，实现极其顺滑的连续缩放效果。
     * 3. 移除方向限制，任何角度的撑开并拢都能触发。
     */
    override fun onScale(scale: Float): Float {
        // 降低阈值（0.05），让操作更灵敏
        if (scale < 0.95f || scale > 1.05f) {
            val oldSize = mCurrentFontSize
            if (scale > 1.0f) {
                mCurrentFontSize++
            } else {
                mCurrentFontSize--
            }
            
            // 限制字号范围在 4 到 40 之间
            mCurrentFontSize = mCurrentFontSize.coerceIn(4, 40)
            
            if (oldSize != mCurrentFontSize) {
                findTerminalView(activity.window.decorView)?.let { view ->
                    view.setTextSize(mCurrentFontSize)
                    // 字号改变后，Termux 需要重算行列并刷新
                    view.post { view.onScreenUpdated() }
                }
            }
            // 返回 1.0f 是关键，它告诉 TerminalView “我已经处理了这次增量”，
            // 这样下次触发时的 scale 就是基于当前状态的新增量，而不是累积量。
            return 1.0f 
        }
        return scale
    }

    private fun findTerminalView(view: android.view.View): TerminalView? {
        if (view is TerminalView) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = findTerminalView(view.getChildAt(i))
                if (child != null) return child
            }
        }
        return null
    }

    override fun shouldEnforceCharBasedInput(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun onLongPress(event: MotionEvent): Boolean = false
}
