package org.github.ewt45.winemulator.emu

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.Consts
import java.io.File

class Proot {
    private val TAG = "Proot"

    suspend fun attach(): ProcessBuilder = withContext(Dispatchers.IO) {
        val rootfs = Consts.rootfsCurrDir
        val prootBin = Consts.prootBin
        prootBin.setExecutable(true)
        
        val userInfo = ProotRootfs.getPreferredUser(rootfs.canonicalFile.name)
        Log.d(TAG, "启动 Proot，目标用户: ${userInfo.name} (UID: ${userInfo.uid})")

        // 1. 核心参数：-0 必须放在首位，强制开启 root 映射
        val prootArgs = mutableListOf(
            prootBin.absolutePath,
            "-0",                     // 必须：Root 映射，解决 I have no name! 问题
            "--link2symlink",         // 必须：解决 Android f2fs 上的文件属性问题
            "--sysvipc",              // 必须：支持进程间通信
            "--kill-on-exit",         // 必须：主进程退出时清理子进程
            "-r", rootfs.absolutePath,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "/storage",
            "-b", "/system",
            "-w", userInfo.home
        )

        // 如果不是登录 root，则使用 change-id
        if (userInfo.uid != 0L) {
            prootArgs.add("--change-id=${userInfo.uid}")
        }

        // 2. 构建纯净的环境变量
        val loginEnvs = mutableMapOf(
            "TERM" to "xterm-256color",
            "HOME" to userInfo.home,
            "USER" to userInfo.name,
            "LOGNAME" to userInfo.name,
            "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "SHELL" to userInfo.shell,
            "LANG" to Consts.Pref.general_rootfs_lang.get(),
            "TMPDIR" to "/tmp",
            "PROOT_NO_SECCOMP" to "1" // 极其重要：解决 Android 12+ 无法伪装 UID 的问题
        )

        // 3. 组装最终命令
        val finalCommand = mutableListOf<String>().apply {
            addAll(prootArgs)
            add("/usr/bin/env")
            add("-i") // 彻底清除宿主环境变量污染
            loginEnvs.forEach { (k, v) -> add("$k=$v") }
            add(userInfo.shell)
            add("-l") // 登录模式，加载 /etc/profile 等
        }

        lastTimeCmd = finalCommand.joinToString(" ")

        return@withContext ProcessBuilder(finalCommand)
            .directory(rootfs)
            .also {
                it.environment().clear()
                // 必须在 ProcessBuilder 层也设置，确保 proot 自身能找到临时目录
                it.environment()["PROOT_TMP_DIR"] = Consts.tmpDir.absolutePath
                it.environment()["PROOT_NO_SECCOMP"] = "1"
            }
            .redirectErrorStream(true)
    }

    companion object {
        var lastTimeCmd = ""
    }
}
