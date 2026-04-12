package org.github.ewt45.winemulator.viewmodel

import android.system.OsConstants
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.emu.Proot
import org.github.ewt45.winemulator.terminal.SessionClientAImpl

class TerminalViewModel : ViewModel() {
    private val TAG = "TerminalViewModel"
    private val terminal: Proot = Proot()
    
    var terminalSession by mutableStateOf<TerminalSession?>(null)
        private set

    var currentUser by mutableStateOf("root")
    var currentHost by mutableStateOf("localhost")
    var currentPath by mutableStateOf("~")
    var isConnected by mutableStateOf(false)

    suspend fun startTerminal(sessionClient: SessionClientAImpl) {
        if (terminalSession != null) return
        
        try {
            val pb = terminal.attach()
            val cmdList = pb.command()
            val executable = cmdList[0]
            val args = cmdList.drop(1).toTypedArray()
            val cwd = pb.directory()?.absolutePath ?: "/"
            
            // 关键修复：从 ProcessBuilder 提取环境变量
            // 如果不传这个 envs，proot 就会因为找不到临时目录而回退到 Termux 默认路径，导致权限报错
            val envs = pb.environment().map { "${it.key}=${it.value}" }.toTypedArray()
            
            withContext(Dispatchers.Main) {
                val session = TerminalSession(
                    executable, 
                    cwd, 
                    args, 
                    envs, // 传入恢复的环境变量
                    2000, 
                    sessionClient
                )
                
                terminalSession = session
                isConnected = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "startTerminal 失败", e)
        }
    }

    fun runCommand(command: String) {
        terminalSession?.write(command + "\n")
    }

    fun updatePromptFromSettings(userName: String) {
        currentUser = userName.ifBlank { "root" }
    }

    fun stopTerminal() {
        terminalSession?.finishIfRunning()
        terminalSession = null
        isConnected = false
    }

    fun pauseTerminal() {
        val pid = terminalSession?.pid ?: -1
        if (pid > 0) android.os.Process.sendSignal(pid, OsConstants.SIGSTOP)
    }

    fun resumeTerminal() {
        val pid = terminalSession?.pid ?: -1
        if (pid > 0) android.os.Process.sendSignal(pid, OsConstants.SIGCONT)
    }

    override fun onCleared() {
        super.onCleared()
        stopTerminal()
    }
}
