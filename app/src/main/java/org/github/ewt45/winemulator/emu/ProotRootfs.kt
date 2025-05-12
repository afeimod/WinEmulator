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
        /** 获取一个rootfs文件夹内/etc/passwd 中存储的用户信息 */
        fun getUserInfos(rootfs: File = Consts.rootfsCurrDir): List<UserInfo> {
            var returnValue:List<UserInfo>
            try {
                returnValue = FileUtils.readLines(File(rootfs, "/etc/passwd"), StandardCharsets.UTF_8).mapNotNull { line ->
                    line.split(":").takeIf { it.size == 7 }?.let {
                        val uid = it[2].toLong()
                        if (uid < 1000L && uid != 0L) return@let null
                        if (uid == 65534L) return@let null
                        return@let UserInfo(it[0], uid, it[3].toLong(), it[5], it[6])
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                returnValue = listOf()
            }
            returnValue.takeIf { returnValue.find { it.name == "root" } == null }?.let {
                returnValue += (UserInfo.ROOT)
            }
            return returnValue
        }

        /**
         * 从/.emuconf 获取当前选择的文件夹名称
         */
        fun getCurrentSelectUser(name:String) {
            TODO("目前是写在了datastore里（json存储）")
        }

        /**
         * 获取某rootfs的所选登陆用户名。
         * 首先从本地json中读取，如果没有，则优先返回非root用户。最后选项是root用户
         * @param rootfsName 作为 [Consts.Pref.Local.rootfs_login_user_json] 的map的key 获取存储的user名。不能为current
         */
        suspend fun getPreferredUser(rootfsName: String):UserInfo = getUserInfos(File(Consts.rootfsAllDir,rootfsName)).run {
            if (rootfsName == Consts.rootfsCurrDir.name) throw IllegalArgumentException("用于搜索的 rootfsName 不能为 'current'")
            val userMap: Map<String, String> = Json.decodeFromString(Consts.Pref.Local.rootfs_login_user_json.get())
            val lastSelectedUserName = userMap[rootfsName]
            var foundInfo = find { info -> info.name == lastSelectedUserName }
            if (foundInfo == null) foundInfo = find { info -> info.name != "root" }
            if (foundInfo == null) foundInfo = find { info -> info.name == "root" }
            if (foundInfo == null) foundInfo = ProotRootfs.UserInfo.ROOT
            return@run foundInfo
        }
    }
}