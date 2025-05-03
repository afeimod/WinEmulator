package org.github.ewt45.winemulator

import android.animation.ValueAnimator
import android.app.Activity
import android.system.Os
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.IOUtils
import org.github.ewt45.winemulator.Consts.alpineRootfsDir
import org.github.ewt45.winemulator.Consts.prootBin
import org.github.ewt45.winemulator.Consts.rootfsAllDir
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.URL
import java.security.MessageDigest


object Utils {
    private const val TAG = "Utils"

    /**
     * 计算sha256的值。比较时注意全部转为大/小写
     */
    suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 8) // 8KB缓冲区
        FileInputStream(file).use { inputStream ->
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val hashBytes = digest.digest()
        // 转换为十六进制字符串
        return@withContext hashBytes.joinToString("") { "%02x".format(it) }
    }




    /**
     * 输入流内容复制到输出流。使用kt的copyTo 会自动使用buffer. autoCLose是否复制完关闭流，默认开启
     */
    fun streamCopy(input: InputStream, output: OutputStream, autoClose: Boolean = true) {
        input.copyTo(output)
        if (autoClose) {
            output.close()
            input.close()
        }
    }

    suspend fun readLinesProcessOutput(process: Process): String = withContext(Dispatchers.IO) {
        val output:String
        BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
            output = lines.joinToString(separator = "\n")
        }
        return@withContext output
    }

    /**
     * 下载链接。
     * @param link http网址
     * @param dstFile 下载为该本地文件
     */
    fun downloadLink(link: String, dstFile: File) {
        val url = URL(link)
        url.openStream().use { input ->
            FileOutputStream(dstFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    object Rootfs {
        /**
         * 检查rootfs是否存在。若不存在，则下载并解压
         */
        suspend fun ensureAlpineRootfs(ctx: Activity):Unit = withContext(Dispatchers.IO) {
            if (!alpineRootfsDir.exists() || alpineRootfsDir.list()?.isEmpty() != false) {
                // 解压到rootfs文件夹，因为压缩包有一层alpine-aarch64文件夹。
                Utils.Archive.decompressTarXz(ctx.assets.open("alpine-aarch64-pd-v4.21.0.tar.xz"), rootfsAllDir)
            }
        }

        /**
         * 将某一个rootfs激活为当前rootfs（之后可通过rootfsCurrDir 获取
         */
        fun makeCurrent(rootfsDir:File) {
            Consts.rootfsCurrDir.delete()
            Os.symlink(rootfsDir.absolutePath, Consts.rootfsCurrDir.absolutePath)
        }
    }

    object Archive {
        /**
         * 解压一个.tar.xz压缩文件
         * @param archiveInputStream 对应压缩文件的输入流
         */
        @Throws(IOException::class)
        fun decompressTarXz(archiveInputStream: InputStream, dstDir: File) {
            if (!dstDir.exists()) dstDir.mkdirs()

            //文件->xz->tar
            XZCompressorInputStream(archiveInputStream).use { xzIn ->
                TarArchiveInputStream(xzIn).use { tis ->
                    var entry: TarArchiveEntry
                    while (tis.nextEntry.also { entry = it } != null) {
                        val name = entry.name
                        val outFile = File(dstDir, name)
                        //确保父目录存在
                        outFile.parentFile?.mkdirs()
                        //如果是目录，创建目录
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        }
                        //如果是符号链接
                        else if (entry.isSymbolicLink) {
                            Os.symlink(entry.linkName, outFile.absolutePath)
//                            Log.d(TAG,"extract: 解压时发现符号链接：链接文件：${entry.name}，指向文件：${entry.linkName}")
                        }
                        //文件，解压
                        else {
                            FileOutputStream(outFile).use { os -> tis.copyTo(os) }
                            Os.chmod(outFile.absolutePath, entry.mode) //不知为何执行权限没同步过来？
                            // FileUtils.copyInputStreamToFile(tis, file); //不能用这个，会自动关闭输入流
                        }



                    }
                }
            }
        }
    }

    object Ui {

        /** 将一个悬浮窗靠向最近的一条边。嵌进去一半. */
        fun View.snapToNearestEdgeHalfway() {
            val parent = parent as? View ?: return
            val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return

            val snapDistanceLeft = left
            val snapDistanceRight = parent.width - right
            val snapDistanceTop = top
            val snapDistanceBottom = parent.height - bottom

            val minDistance = minOf(snapDistanceLeft, snapDistanceRight, snapDistanceTop, snapDistanceBottom)

            val currentLeft = left
            val currentTop = top
            var targetLeft = currentLeft
            var targetTop = currentTop

            when (minDistance) {
                snapDistanceLeft -> targetLeft = -width / 2
                snapDistanceRight -> targetLeft = parent.width - width / 2
                snapDistanceTop -> targetTop = -height / 2
                snapDistanceBottom -> targetTop = parent.height - height / 2
            }

            ValueAnimator.ofInt(currentLeft, targetLeft).apply {
                duration = 300
                addUpdateListener { animation ->
                    lp.leftMargin = animation.animatedValue as Int
                    requestLayout()
                }
            }.start()

            ValueAnimator.ofInt(currentTop, targetTop).apply {
                duration = 300
                addUpdateListener { animation ->
                    lp.topMargin = animation.animatedValue as Int
                    requestLayout()
                }
            }.start()
        }

        /**
         * 用于viewmodel中将 从datastore获取到的flow 转为stateflow
         */
        fun <T> ViewModel.stateInSimple(initValue:T, flow:Flow<T>):StateFlow<T> {
            return flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initValue)
        }

        /**
         * 用于viewmodel中修改datastore的数据
         */
        fun <T> ViewModel.editDateStore(key:Preferences.Key<T>, value: T) {
            viewModelScope.launch { dataStore.edit { it[key] = value} }
        }
    }
}