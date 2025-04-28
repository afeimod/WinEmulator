package org.github.ewt45.winemulator

import android.content.Context
import android.system.Os
import android.system.OsConstants
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream

object Consts {
    lateinit var cacheDir:File

    /** 用于proot绑定 /tmp 的安卓路径 */
    lateinit var tmpDir: File
    lateinit var rootfsDir:File
    lateinit var alpineRootfsDir: File
    lateinit var prootBin : File

    /**
     * 初始化。使用前先调用一次
     */
    fun init(ctx:Context) {
        cacheDir = ctx.cacheDir
        cacheDir.mkdirs()

        tmpDir = File(cacheDir, "tmp")
        tmpDir.mkdirs()
        val mode777 = OsConstants.S_IRWXU or OsConstants.S_IRWXG or OsConstants.S_IRWXO
//        Os.chmod(tmpDir.absolutePath, 0777)

        val fileDir = ctx.filesDir
        rootfsDir = File(fileDir, "rootfs")
        rootfsDir.mkdirs()

        alpineRootfsDir = File(rootfsDir, "alpine-aarch64") //这个等解压的时候再创建吧

        //proot从assets解压
        prootBin = File(fileDir, "proot")
        if (!prootBin.exists()) {
            Utils.streamCopy(ctx.assets.open("proot"), FileOutputStream(prootBin))
        }
        prootBin.setExecutable(true)
    }
}