package org.github.ewt45.winemulator.emu

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.Consts.Pref.general_shared_ext_path
import org.github.ewt45.winemulator.Consts.Pref.proot_bool_options
import org.github.ewt45.winemulator.Consts.tmpDir
import org.github.ewt45.winemulator.Consts.rootfsCurrL2sDir
import org.github.ewt45.winemulator.Utils.chmod
import java.io.File
import java.nio.charset.StandardCharsets

class Proot {
    private val TAG = "Proot"

    /** proot 容器内用户的 home 目录，用于 TerminalSession 的 cwd */
    var containerCwd = "/"
        private set

    suspend fun attach(): ProcessBuilder = withContext(Dispatchers.IO) {
        val rootfs = Consts.rootfsCurrDir
        val prootBin = Consts.prootBin
        prootBin.setExecutable(true)
        
        val lang = Consts.Pref.general_rootfs_lang.get()
        
        // 确保 link2symlink 目录存在
        rootfsCurrL2sDir.mkdirs()
        chmod(rootfsCurrL2sDir, "755")
        
        ProotHelper.setup_fake_data()
        editEtcLocaleGen(rootfs, lang)
        
        val userInfo = ProotRootfs.getPreferredUser(rootfs.canonicalFile.name)
        Log.d(TAG, "启动 Proot，目标用户: ${userInfo.name} (UID: ${userInfo.uid}, GID: ${userInfo.gid})")
        
        // 记录容器内的 cwd（proot 容器内的路径，不是 Android 路径）
        containerCwd = userInfo.home

        // 确保用户的 HOME 目录在 rootfs 中存在并有正确权限
        val homeDir = File(rootfs, userInfo.home)
        if (!homeDir.exists()) {
            homeDir.mkdirs()
            Log.d(TAG, "创建 HOME 目录: ${homeDir.absolutePath}")
        }
        // 确保 HOME 目录有正确的权限（特别是 root 用户）
        homeDir.setReadable(true, false)
        homeDir.setWritable(true, false)
        homeDir.setExecutable(true, false)

        // 1. 核心参数 - 使用用户设置的 proot 参数
        val prootArgs = mutableListOf(
            prootBin.absolutePath,
            *proot_bool_options.get().toTypedArray(),  // 用户自定义参数
            "--kernel-release=${ProotHelper.DEFAULT_FAKE_KERNEL_VERSION}",  // 伪装内核版本
            "--rootfs=${rootfs.absolutePath}",
            "--change-id=${userInfo.uid}:${userInfo.gid}",  // 关键：包含 gid
            // 注意：不使用 --cwd 参数，让 HOME 环境变量和 shell -l 来处理初始目录
            "--bind=${tmpDir.absolutePath}:/tmp",
            "--bind=${rootfs.absolutePath}/tmp:/dev/shm",
            "--bind=/sys",
            "--bind=/proc/self/fd:/dev/fd",
            "--bind=/proc",
            "--bind=/dev/urandom:/dev/random",
            "--bind=/dev",
        )

        // 绑定标准文件描述符（如果不存在）
        File("/dev/stderr").takeIf { !it.exists() }?.let {
            prootArgs.add("--bind=/proc/self/fd/2:/dev/stderr")
        }
        File("/dev/stdout").takeIf { !it.exists() }?.let {
            prootArgs.add("--bind=/proc/self/fd/1:/dev/stdout")
        }
        File("/dev/stdin").takeIf { !it.exists() }?.let {
            prootArgs.add("--bind=/proc/self/fd/0:/dev/stdin")
        }

        // SELinux 伪装
        ProotHelper.setup_fake_data()
        prootArgs.add("--bind=${rootfs.absolutePath}/sys/.empty:/sys/fs/selinux")

        // /proc 伪装绑定 - 提供伪造的系统信息
        prootArgs.addAll(
            mapOf(
                "/proc/.loadavg" to "/proc/loadavg",
                "/proc/.stat" to "/proc/stat",
                "/proc/.uptime" to "/proc/uptime",
                "/proc/.version" to "/proc/version",
                "/proc/.vmstat" to "/proc/vmstat",
                "/proc/.sysctl_entry_cap_last_cap" to "/proc/sys/kernel/cap_last_cap",
                "/proc/.sysctl_inotify_max_user_watches" to "/proc/sys/fs/inotify/max_user_watches",
            ).mapNotNull { bindIfNotReadable(rootfs, it.key, it.value) })

        // 动态绑定用户配置的外部存储路径
        prootArgs.addAll(general_shared_ext_path.get().map { bindPath ->
            File(rootfs, bindPath).runCatching { takeIf { FileUtils.isSymlink(it) }?.delete() }
            "--bind=$bindPath"
        })

        // 2. 构建环境变量 - 先读取 /etc/environment
        val loginEnvs = EnvMap()
        readEtcEnvironment(rootfs, loginEnvs)
        
        // 覆盖/添加必要的环境变量
        loginEnvs.put("LANG", lang, true)
        loginEnvs.put("HOME", userInfo.home, true)
        loginEnvs.put("USER", userInfo.name, true)
        loginEnvs.put("LOGNAME", userInfo.name, true)
        loginEnvs.put("TMPDIR", "/tmp", true)
        loginEnvs.put("DISPLAY", ":13", true)
        loginEnvs.put("PULSE_SERVER", "tcp:127.0.0.1:4713", true)
        loginEnvs.put("TERM", "xterm-256color", true)
        loginEnvs.put("SHELL", userInfo.shell, true)
        loginEnvs.put("PROOT_NO_SECCOMP", "1", true)

        // 3. 组装最终命令
        val finalCommand = mutableListOf<String>().apply {
            addAll(prootArgs)
            add("/usr/bin/env")
            add("-i")  // 彻底清除宿主环境变量污染
            loginEnvs.toArray().forEach { add(it) }
            add(userInfo.shell)
            add("-l")  // 登录模式，加载 /etc/profile 等
        }

        lastTimeCmd = finalCommand.joinToString(" ")
        Log.d(TAG, "attach: 最终命令=$lastTimeCmd")

        return@withContext ProcessBuilder(finalCommand)
            .directory(rootfs)
            .also {
                it.environment().clear()  // 清除 Android 环境变量污染
                it.environment()["PROOT_TMP_DIR"] = tmpDir.absolutePath
                it.environment()["PROOT_NO_SECCOMP"] = "1"
                it.environment()["LD_PRELOAD"] = ""  // 防止库冲突
            }
            .redirectErrorStream(true)
    }

    /**
     * 读取 /etc/environment 下的环境变量并添加到 envMap
     */
    private fun readEtcEnvironment(rootfs: File, envMap: EnvMap) {
        try {
            for (line in File(rootfs, "/etc/environment").readLines()) {
                val trimmed = line.trim()
                if (!trimmed.startsWith('#') && trimmed.contains('=')) {
                    val split = trimmed.split('=', limit = 2)
                    envMap.put(split[0], split[1].trim('"'))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "读取 /etc/environment 失败: ${e.message}")
        }
    }

    /**
     * 编辑 /etc/locale.gen，启用对应语言
     */
    private fun editEtcLocaleGen(rootfs: File, targetLocale: String) {
        try {
            val file = File(rootfs, "/etc/locale.gen").takeIf { it.exists() } ?: return
            val regexCharNum = "[^a-zA-Z0-9]".toRegex()
            val lines = FileUtils.readLines(file, StandardCharsets.UTF_8).map { line ->
                val uncommentLine = line.trimStart('#').trim()
                val locale = uncommentLine.split(' ').takeIf { it.size == 2 }?.get(0) ?: return@map line
                val comp1 = locale.replace(regexCharNum, "").lowercase()
                val comp2 = targetLocale.replace(regexCharNum, "").lowercase()
                return@map if (comp1 == comp2) uncommentLine else line
            }
            FileUtils.writeLines(file, lines)
        } catch (e: Exception) {
            Log.w(TAG, "编辑 locale.gen 失败: ${e.message}")
        }
    }

    /**
     * 如果文件存在且可读，返回 null；否则返回绑定参数
     */
    private fun bindIfNotReadable(rootfs: File, bindFrom: String, bindTo: String): String? {
        return try {
            File(bindTo).takeUnless { it.exists() && it.canRead() }?.let { 
                "--bind=${File(rootfs, bindFrom).absolutePath}:$bindTo" 
            }
        } catch (e: Exception) {
            "--bind=${File(rootfs, bindFrom).absolutePath}:$bindTo"
        }
    }

    companion object {
        var lastTimeCmd = ""
    }
}

/**
 * 环境变量 Map，支持拼接或覆盖
 */
class EnvMap {
    val map = mutableMapOf<String, String>()

    /**
     * 新增/更改环境变量。将 value 放在现有 value 之前。如果 override 为 true 则替换
     */
    fun put(k: String, v: String, override: Boolean = false) {
        val k1 = k.trim()
        val v1 = v.trim()
        if (k1.contains("=")) Log.w("EnvMap", "key 不应包含 =: key=$k1 value=$v1")
        val oldV = map[k1]
        map[k1] = if (oldV != null && !override) "$v1:$oldV" else v1
    }

    fun get(k: String): String = map.getOrDefault(k, "")

    /** 返回数组，每个元素是 k=v 格式 */
    fun toArray(): Array<String> = map.toList().map { "${it.first}=${it.second}" }.toTypedArray()
}
