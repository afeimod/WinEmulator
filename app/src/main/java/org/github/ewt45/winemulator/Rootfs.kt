package org.github.ewt45.winemulator

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.Consts.alpineRootfsDir
import org.github.ewt45.winemulator.Consts.rootfsDir
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL

object Rootfs {
    /**
     * 检查rootfs是否存在。若不存在，则下载并解压
     */
    suspend fun ensure(ctx: Activity):Unit = withContext(Dispatchers.IO) {

        //判断条件：rootfs文件夹存在且内容不为空
        if (rootfsDir.exists() && rootfsDir.isDirectory
            && rootfsDir.list()?.isEmpty() == false) {
            return@withContext
        }
        if (!alpineRootfsDir.exists() || alpineRootfsDir.list()?.isEmpty() != false) {
            Utils.decompressTarXz(ctx.assets.open("alpine-aarch64-pd-v4.21.0.tar.xz"), rootfsDir) // 解压到rootfs文件夹，因为压缩包有一层alpine-aarch64文件夹。
        }

//        val rootfsFile = File(Consts.cacheDir, "rootfs.tar.gz")
//        if (rootfsFile.exists()) {
//            if (Utils.calculateSha256(rootfsFile).lowercase() == "140afac5b74f614c2b746bb8e2a312dbb1cc34650b8a5afbfcc424491f32cf4f")
//                return@withContext
//            else
//                rootfsFile.delete()
//        }

//        //下载
//        val url = URL("https://github.com/termux/proot-distro/releases/download/v4.21.0/alpine-aarch64-pd-v4.21.0.tar.xz")
//        url.openStream().use { input ->
//            FileOutputStream(rootfsFile).use { output ->
//                input.copyTo(output)
//            }
//        }
    }

    /**
     * 启动proot 使用sh执行一条命令。返回输出
     */
    suspend fun runProotCommand(command: String): String {
        val rootfs = Consts.alpineRootfsDir

        val processBuilder = ProcessBuilder(
            Consts.prootBin.absolutePath,
            "-0",
            "-L",
            "--link2symlink",
            "--kill-on-exit",
            "--rootfs=${rootfs}",
            "--cwd=/",
            "--bind=/dev",
            "--bind=${Consts.cacheDir}:/tmp",
            "--bind=/proc",
            "--bind=/sys",
            "--bind=/storage/emulated/0/Download",
            "/usr/bin/env",
            "TMPDIR=/tmp LC_ALL=zh_CN.utf8",
            "DISPLAY=:0",
            "PATH=/opt/wine/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu:/usr/lib/arm-linux-gnueabihf",
            "/bin/sh", "-c", command
        )
            .directory(rootfs)
            .also {
                it.environment()["PROOT_TMP_DIR"] = Consts.tmpDir.absolutePath
                it.environment()["LD_PRELOAD"] = ""
            }
            .redirectErrorStream(true)

        val process = processBuilder.start()
        val output = Utils.readLinesProcessOutput(process)
        process.waitFor()
        return output
    }



}