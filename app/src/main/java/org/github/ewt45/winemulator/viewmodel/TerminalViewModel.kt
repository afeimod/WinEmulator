package org.github.ewt45.winemulator.viewmodel

import android.system.OsConstants.SIGCONT
import android.system.OsConstants.SIGSTOP
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val output = mutableStateOf<List<String>>(emptyList())
    private val outputMutex = Mutex() //锁，修改output相关内容时应该使用

    /** 当前用户名 */
    var currentUser by mutableStateOf("root")
        private set

    /** 当前主机名 */
    var currentHost by mutableStateOf("localhost")
        private set

    /** 当前路径 */
    var currentPath by mutableStateOf("~")
        private set

    /** 连接状态 */
    var isConnected by mutableStateOf(false)
        private set

    /** 美化的命令提示符格式: 用户名@主机:路径$ */
    private val promptPrefix: String
        get() = "$currentUser@$currentHost:$currentPath\$ "

    /**
     * 启动终端
     */
    suspend fun startTerminal() {
        if (process != null) return
        isConnected = true
        
        process = withContext(Dispatchers.IO) {
            terminal.attach().start()
        }

        //绑定输入输出
        processWriter = OutputStreamWriter(process!!.outputStream)

        //另起协程获取输出以及等待关闭
        viewModelScope.launch(Dispatchers.IO) {
            updateOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            updateOutput("  终端开始运行")
            updateOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            updateOutput("")
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
                                // 如果500ms内字符输出没有更新过，则将当前缓存的无换行字符串显示出来。
                                if (lastReadCharTime == lastReadCharTimeCopy && lastReadCharTimeCopy != 0L && builder.isNotEmpty()) {
                                    val lastLine = output.lastOrNull()
                                    if (lastLine?.endsWith('\n') != false) updateOutput(builder.toString())
                                    else updateOutputAtLast(builder.toString())
                                    builder.clear()
                                }
                                lastReadCharTimeCopy = lastReadCharTime
                            }
                        }
                    }
                    
                    // 解析路径变化的正则
                    val pathRegex = Regex("""\~?([/\w.-]*)""")
                    while (reader.read().also { readInt = it } != -1) {
                        charRead = readInt.toChar()
                        outputMutex.withLock {
                            lastReadCharTime = System.currentTimeMillis()
                            builder.append(charRead)
                            
                            // 尝试解析路径（简单的启发式方法）
                            if (charRead == ':' && builder.length > 2) {
                                val potentialPath = builder.toString().takeLast(50)
                                if (potentialPath.matches(Regex(""".*:[/~][/\w.-]*\$?"""))) {
                                    currentPath = extractPath(potentialPath)
                                }
                            }
                            
                            if (charRead == '\n') {
                                val line = builder.toString()
                                // 检测用户名变化
                                detectUserChange(line)
                                
                                // 限制输出数量
                                val currentList = output.value.toMutableList()
                                if (currentList.size > 800) {
                                    currentList.removeRange(0, 400)
                                }
                                currentList.add(line)
                                output.value = currentList
                                builder.clear()
                            }
                        }
                    }
                    updateInlineOutputJob.cancel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateOutput("错误: ${e.message}")
            }
            process?.waitFor()
            isConnected = false
            closeResources()
        }

        if (Proot.lastTimeCmd.isNotBlank())
            updateOutput("使用以下参数启动proot：")
            updateOutput(Proot.lastTimeCmd)
            updateOutput("")
        return
    }

    /**
     * 从提示符行中提取路径
     */
    private fun extractPath(text: String): String {
        // 尝试匹配 ~ 或 / 开头的路径
        val pathMatch = Regex(""":([/~][/\w.-]*)[\$#]""").find(text)
        return pathMatch?.groupValues?.get(1) ?: "~"
    }

    /**
     * 检测用户名是否变化
     */
    private fun detectUserChange(line: String) {
        // 检测 su - username 或 sudo -i 等命令后的用户变化
        if (line.contains("su -") || line.contains("sudo -i")) {
            val userMatch = Regex("""su\s+-\s*(\w+)""").find(line)
                ?: Regex("""sudo\s+-i""").find(line)
            // 简化处理：假设切换到root
            if (userMatch != null) {
                currentUser = "root"
            }
        }
        // 检测 whoami 输出
        if (line.contains("root") && output.value.size > 5) {
            // 检查前几行是否有 whoami 命令
            val recentLines = output.value.takeLast(5)
            if (recentLines.any { it.contains("whoami") }) {
                currentUser = "root"
            }
        }
    }

    /**
     * 更新输出
     */
    private fun updateOutput(line: String) {
        val currentList = output.value.toMutableList()
        currentList.add(line)
        output.value = currentList
    }

    /**
     * 在最后一行追加内容
     */
    private fun updateOutputAtLast(additional: String) {
        val currentList = output.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val lastIndex = currentList.lastIndex
            currentList[lastIndex] = currentList[lastIndex] + additional
            output.value = currentList
        } else {
            currentList.add(additional)
            output.value = currentList
        }
    }

    /**
     * 执行某个命令
     * @param display 为false时不显示在屏幕上
     */
    fun runCommand(command: String, display: Boolean = true) = viewModelScope.launch(Dispatchers.IO) {

        if (processWriter == null || process?.isAlive != true) {
            updateOutput("进程已关闭。无法执行命令 $command")
            stopTerminal()
            return@launch
        }

        if (display) {
            // 使用美化的提示符
            updateOutput(promptPrefix + command)
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
     * 设置当前用户
     */
    fun setCurrentUser(user: String) {
        currentUser = user
    }

    /**
     * 设置当前主机名
     */
    fun setCurrentHost(host: String) {
        currentHost = host
    }

    /**
     * 设置当前路径
     */
    fun setCurrentPath(path: String) {
        currentPath = path
    }

    /**
     * 更新提示符信息（从设置读取）
     */
    fun updatePromptFromSettings(userName: String) {
        currentUser = userName.ifBlank { "root" }
    }

    /**
     * 结束终端
     */
    fun stopTerminal() {
        isConnected = false
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
        isConnected = false
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
    }

    fun resumeTerminal() {
        val pid = process?.getPid() ?: -1
        android.os.Process.sendSignal(pid, SIGCONT)
    }
}
