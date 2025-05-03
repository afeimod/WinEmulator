package org.github.ewt45.winemulator

import android.app.Activity
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object Consts {
    lateinit var cacheDir:File

    /** 用于proot绑定 /tmp 的安卓路径 */
    lateinit var tmpDir: File
    /** 此文件夹内包含各种rootfs. files/rootfs */
    lateinit var rootfsAllDir:File
    /** 当前激活的rootfs, 应该为一个指向实际rootfs的软链接. files/rootfs/current */
    lateinit var rootfsCurrDir: File
    /** 一个用于测试的alpine rootfs. files/rootfs/alpine-aarch64 */
    lateinit var alpineRootfsDir: File
    /** proot二进制文件. files/proot  */
    lateinit var prootBin : File

    object Ui {
        /** 最小化时的宽高dp值 */
        val minimizedIconSize = 48
    }

    /**
     * 用户偏好相关.
     * 如果assets中指定了默认值，会覆盖这里的默认值
     */
    object Pref {
        val key_proot_bool_options = stringSetPreferencesKey("proot_bool_options")
        var default_proot_bool_options =
            setOf("--root-id", "-L", "--link2symlink", "--kill-on-exit")
        val key_proot_startup_cmd = stringPreferencesKey("proot_startup_cmd")
        var default_proot_startup_cmd = ""
    }

    /**
     * 初始化。使用前先调用一次
     */
    fun init(ctx:Context) {
        cacheDir = ctx.cacheDir
        cacheDir.mkdirs()

        tmpDir = File(cacheDir, "tmp")
        tmpDir.mkdirs()
//        Os.chmod(tmpDir.absolutePath, 0777)

        val fileDir = ctx.filesDir
        rootfsAllDir = File(fileDir, "rootfs")
        rootfsAllDir.mkdirs()

        rootfsCurrDir = File(rootfsAllDir, "current")

        alpineRootfsDir = File(rootfsAllDir, "alpine-aarch64") //这个等解压的时候再创建吧

        //proot从assets解压
        prootBin = File(fileDir, "proot")
        if (!prootBin.exists()) {
            Utils.streamCopy(ctx.assets.open("proot"), FileOutputStream(prootBin))
        }
        prootBin.setExecutable(true)
    }
}

