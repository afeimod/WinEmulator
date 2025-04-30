package org.github.ewt45.winemulator.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.EmulatorTerminal
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TerminalViewModel : ViewModel() {
    private val terminal: EmulatorTerminal = EmulatorTerminal()
    private var process : Process? = null

    /** 输入 */
    private var processWriter: OutputStreamWriter? = null

    val output = mutableStateListOf<String>()

    /**
     * 启动终端
     */
    fun startTerminal() {
        if (process != null) return

        viewModelScope.launch(Dispatchers.IO) {
            process = terminal.attach().start()
            //绑定输入输出
            processWriter = OutputStreamWriter(process!!.outputStream)
            launch {
                output.add("终端开始运行")
                try {
                    BufferedReader(InputStreamReader(process!!.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            output.takeIf { it.size > 800 }?.removeRange(0, 400)
                            output.add(line!!)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            process!!.waitFor()
            closeResources()
        }
    }

    /**
     * 执行某个命令
     */
    fun runCommand(command: String) {
        output.add("$ " + command)
        if (process != null && !process!!.isAlive) {
            stopTerminal()
        }
        if (processWriter == null) {
            output.add("进程已关闭")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 添加回车，否则不会执行
                processWriter?.write(command + "\n")
                // 确保命令立刻发送
                processWriter?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 结束终端
     */
    fun stopTerminal() {
//        viewModelScope.launch(Dispatchers.IO) {
//        }
        closeResources()
    }

    /**
     * 清理资源
     */
    private fun closeResources() {
        try { processWriter?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.outputStream?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.inputStream?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.errorStream?.close() } catch (e: Exception) { /* Ignore */ }
        try { process?.destroy() } catch (e: Exception) { /* Ignore */ }
        process = null
        processWriter = null
    }


    /**
     * viewModel销毁时结束终端
     */
    override fun onCleared() {
        super.onCleared()
        stopTerminal()
    }
}
