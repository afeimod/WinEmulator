package org.github.ewt45.winemulator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter

/**
 * 连接linux的终端。输入命令或获取输出
 */
class EmulatorTerminal {
    suspend fun attach(): ProcessBuilder =  withContext(Dispatchers.IO){
        //TODO 每次启动前清空tmpDir？优先bash然后sh？
        val rootfs = Consts.rootfsCurrDir
        val processBuilder = ProcessBuilder(
            Consts.prootBin.absolutePath,
            "--root-id", // root用户登录
            "-L",
            "--link2symlink",
            "--kill-on-exit",
            "--rootfs=${rootfs.absolutePath}",
            "--cwd=/",
            "--bind=/dev",
            "--bind=/proc",
            "--bind=/sys",
            "--bind=${Consts.tmpDir.absolutePath}:/tmp",
            "--bind=/storage/emulated/0/Download",
            "/usr/bin/env",
            "TMPDIR=/tmp",
            "LC_ALL=en_US.utf8",
//            "LC_ALL=zh_CN.utf8",
            "DISPLAY=:13",
            "PATH=/opt/wine/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu:/usr/lib/arm-linux-gnueabihf",
            "/bin/sh", "-l", // -l: 交互式shell，-c: 执行某命令并退出
        )
            .directory(rootfs)
            .also {
                it.environment()["PROOT_TMP_DIR"] = Consts.tmpDir.absolutePath
                it.environment()["LD_PRELOAD"] = ""
            }
            .redirectErrorStream(true)

        return@withContext processBuilder

    }
}