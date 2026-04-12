package org.github.ewt45.winemulator.emu

import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.github.ewt45.winemulator.Consts
import java.io.File
import java.nio.charset.StandardCharsets

class ProotRootfs {
    data class UserInfo(
        val name: String,
        val uid: Long,
        val gid: Long,
        val home: String,
        val shell: String,
    ) {
        companion object {
            val ROOT = UserInfo("root", 0L, 0L, "/root", "/bin/sh")
        }
    }

    companion object {
        fun getUserInfos(rootfs: File): List<UserInfo> {
            var returnValue: List<UserInfo>
            try {
                returnValue = FileUtils.readLines(File(rootfs, "/etc/passwd"), StandardCharsets.UTF_8).mapNotNull { line ->
                    line.split(":").takeIf { it.size == 7 }?.let {
                        val uid = it[2].toLong()
                        // 过滤掉系统预留 UID，但保留 root (0)
                        if (uid < 1000L && uid != 0L) return@let null
                        if (uid == 65534L) return@let null // 过滤 nobody
                        return@let UserInfo(it[0], uid, it[3].toLong(), it[5], it[6])
                    }
                }
            } catch (e: Exception) {
                returnValue = listOf()
            }
            if (returnValue.find { it.name == "root" } == null)
                returnValue += (UserInfo.ROOT)
            return returnValue.filter { !it.name.startsWith("aid_") }.sortedBy { it.name }
        }

        suspend fun getPreferredUser(rootfsName: String): UserInfo {
            if (rootfsName == Consts.rootfsCurrDir.name) throw IllegalArgumentException("不能对 current 进行用户搜索")
            val userMap: Map<String, String> = Json.decodeFromString(Consts.Pref.Local.rootfs_login_user_json.get())
            return getPreferredUser(userMap[rootfsName], getUserInfos(File(Consts.rootfsAllDir, rootfsName)))
        }

        /**
         * 优化后的用户检测：
         * 1. 优先使用上次选中的
         * 2. 其次强制使用 root (UID 0)
         * 3. 实在没有才随机找一个
         */
        fun getPreferredUser(lastSelectedUserName: String?, allUsers: List<UserInfo>): UserInfo {
            // 优先查找上次手动选中的用户
            allUsers.find { it.name == lastSelectedUserName }?.let { return it }
            // 默认优先返回 root 用户
            allUsers.find { it.name == "root" }?.let { return it }
            // 保底返回第一个可用用户或硬编码的 root
            return allUsers.firstOrNull() ?: UserInfo.ROOT
        }
    }
}
