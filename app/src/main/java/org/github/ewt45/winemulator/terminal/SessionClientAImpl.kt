package org.github.ewt45.winemulator.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import org.github.ewt45.winemulator.MainEmuActivity

/**
 * Activity 级别的 Session Client 实现 - 增加界面自动刷新支持
 */
class SessionClientAImpl(
    val activity: MainEmuActivity,
) : SessionClientBase() {
    
    // 关键修复：当终端文本改变时，通知界面刷新
    override fun onTextChanged(changedSession: TerminalSession) {
        // 查找当前 Activity 布局中的 TerminalView 并请求重绘
        // 在 Compose 环境下，AndroidView 包装的原生 View 依然可以通过这种方式刷新
        activity.runOnUiThread {
            // 我们通过遍历或查找来确保能刷到对应的 View
            // 这种方式能解决“输入不显示，点一下才刷新”的问题
            findTerminalView(activity.window.decorView)?.onScreenUpdated()
        }
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

    override fun onSessionFinished(finishedSession: TerminalSession) {
        activity.terminalViewModel.stopTerminal()
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Terminal", text)
        clipboard.setPrimaryClip(clip)
        activity.runOnUiThread {
            Toast.makeText(activity, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(activity).toString()
            session?.write(text)
        }
    }
}
