package org.github.ewt45.winemulator.viewmodel

import android.system.OsConstants.SIGCONT
import android.system.OsConstants.SIGSTOP
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.Utils.getPid
import org.github.ewt45.winemulator.emu.Proot
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TerminalViewModel : ViewModel() {
    private val TAG = "TerminalViewModel"
    private val terminal: Proot = Proot()
    private var process: Process? = null

    /** 输入 */
    private var processWriter: OutputStreamWriter? = null

    /** 输出行。每个字符串代表一行，换行字符包括在字符串结尾，拼接时不应再添加 */
    val output = mutableStateListOf<String>()
    private val outputMutex = Mutex() //锁，修改output相关内容时应该使用

    /**
     * 启动终端
     */
    suspend fun startTerminal() {
        if (process != null) return
        process = withContext(Dispatchers.IO) {
            terminal.attach().start()
        }

        //绑定输入输出
        processWriter = OutputStreamWriter(process!!.outputStream)

        //另起协程获取输出以及等待关闭
        viewModelScope.launch(Dispatchers.IO) {
            output.add("终端开始运行\n")
            try {
                BufferedReader(InputStreamReader(process!!.inputStream)).use { reader ->
                    val builder = StringBuilder()
                    var readInt: Int
                    var charRead: Char
                    var lastReadCharTime = 0L //上次读取到新输出字符的时间。即使不完成整行 也会更新
                    //builder lastUpdateTime output 应该在锁下进行

                    // FIXME adduser 最后一条确认没显示出来？
                    val updateInlineOutputJob = launch {
                        var lastReadCharTimeCopy = 0L
                        while (process?.isAlive == true) {
                            delay(500)
                            outputMutex.withLock {
//                                    Log.d(TAG, "startTerminal: 检测缓存字符串：${lastReadCharTime == lastReadCharTimeCopy} ${builder.isNotEmpty()} 字符串=${builder.toString()}")
                                // 如果500ms内字符输出没有更新过，则将当前缓存的无换行字符串显示出来。
                                if (lastReadCharTime == lastReadCharTimeCopy && lastReadCharTimeCopy != 0L && builder.isNotEmpty()) {
                                    val lastLine = output.lastOrNull()
                                    if (lastLine?.endsWith('\n') != false) output.add(builder.toString())
                                    else output[output.lastIndex] = lastLine + builder.toString()
                                    builder.clear()
                                }
                                lastReadCharTimeCopy = lastReadCharTime
                            }
                        }
                    }
                    while (reader.read().also { readInt = it } != -1) {
                        charRead = readInt.toChar()
                        outputMutex.withLock {
                            lastReadCharTime = System.currentTimeMillis()
                            builder.append(charRead)
                            if (charRead == '\n') {
                                output.takeIf { it.size > 800 }?.removeRange(0, 400)
                                output.add(builder.toString())
                                builder.clear()
                            }
                        }
                    }
                    updateInlineOutputJob.cancel()
                }
                // 旧方法 直接按行读取
//                    BufferedReader(InputStreamReader(process!!.inputStream)).use { reader ->
//                        var line: String?
//                        while (reader.readLine().also { line = it } != null) {
//                            output.takeIf { it.size > 800 }?.removeRange(0, 400)
//                            output.add(line!!)
//                        }
//                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            process?.waitFor()
            closeResources()
        }

        if (Proot.lastTimeCmd.isNotBlank())
            output.add("使用以下参数启动proot：\n${Proot.lastTimeCmd}\n\n")
        return
    }

    /**
     * 执行某个命令
     * @param display 为false时不显示在屏幕上
     */
    fun runCommand(command: String, display: Boolean = true) = viewModelScope.launch(Dispatchers.IO) {

        if (processWriter == null || process?.isAlive != true) {
            output.add("进程已关闭。无法执行命令 $command。\n")
            stopTerminal()
            return@launch
        }

        outputMutex.takeIf { display }?.withLock {
            val shouldNewLine = output.lastOrNull()?.endsWith('\n') ?: true
            output.add((if (shouldNewLine) "$ " else "") + "$command\n") //如果当前未换行则添加到当前行结尾，否则新起一行
        }

        try {
            // 添加回车，否则不会执行
            processWriter?.write(command + "\n")
            // 确保命令立刻发送
            processWriter?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
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
        try {
            processWriter?.close()
            process?.outputStream?.close()
            process?.inputStream?.close()
            process?.errorStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun pauseTerminal() {
        val pid = process?.getPid() ?: -1
        android.os.Process.sendSignal(pid, SIGSTOP)
//        Runtime.getRuntime().exec(arrayOf("kill", "-STOP", "$pid"))
    }

    fun resumeTerminal() {
        val pid = process?.getPid() ?: -1
        android.os.Process.sendSignal(pid, SIGCONT)
//        Runtime.getRuntime().exec(arrayOf("kill", "-STOP", "$pid"))
    }
}
