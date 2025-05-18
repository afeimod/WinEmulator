package org.github.ewt45.winemulator

import android.Manifest
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.system.Os
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okio.Buffer
import okio.GzipSource
import okio.source
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.utils.InputStreamStatistics
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.github.ewt45.winemulator.Consts.Pref.Local.curr_rootfs_name
import org.github.ewt45.winemulator.Consts.rootfsAllDir
import org.github.ewt45.winemulator.Consts.rootfsCurrDir
import org.tukaani.xz.XZ
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
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible


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
        val output: String
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

    fun createShareTextIntent(text: String): Intent {
        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
    }

    /** 获取进程的pid */
    fun Process.getPid(): Int {
        try {
            val property = this::class.declaredMemberProperties.filterIsInstance<KProperty1<Process, Int>>().find { it.name == "pid" }
            if (property == null) return -1
            property.isAccessible = true
            val pid = property.get(this)
            property.isAccessible = false
            return pid
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    /**
     * 获取x11Service对应的进程pid。原理：通过对比进程名（在Manifest中设置的）
     */
    fun Context.getX11ServicePid(): Int {
        return getSystemService(ActivityManager::class.java).runningAppProcesses
            .find { it.processName == "$packageName:xserver" }?.pid ?: -1
    }

    /**
     * chmod. 添加try catch 传入mode为8进制数字的字符串，例如“755”
     */
    fun chmod(file: File, mode: String) {
        try {
            Os.chmod(file.absolutePath, mode.toInt(8))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun File.notExists(): Boolean = !this.exists()


    fun printStackTraceToString(e: Throwable): String = e.stackTraceToString()

    /** 检查文件头是否为给定标识. 从输入流当前位置开始读取 header.size 个字节并进行比较。 */
    private fun InputStream.checkHeaderMagic(header: ByteArray): Boolean {
        try {
            val len = header.size.toLong()
            val fileHeader = source().use { source -> Buffer().also { source.read(it, len) }.readByteArray(len) }
            for (i in 0 until len.toInt())
                if (header[i] != fileHeader[i])
                    return false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /** 判断该文件是否为gz压缩包 */
    fun InputStream.isGzip(): Boolean = checkHeaderMagic(byteArrayOf(0x1F.toByte(), 0x8B.toByte()))

    /** 判断该文件是否为xz压缩包 */
    fun InputStream.isXz(): Boolean = checkHeaderMagic(XZ.HEADER_MAGIC)

    /** 打开uri的输入流。等于 contentResolver.openInputStream(uri) */
    fun Context.openInput(uri: Uri): InputStream? = contentResolver.openInputStream(uri)

    object Files {
        suspend fun writeToUri(ctx: Context, uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val result = ctx.contentResolver.openOutputStream(uri)?.use { output ->
                    IOUtils.write(content, output, StandardCharsets.UTF_8)
                }
                if (result == null)
                    throw RuntimeException("无法获取文件输出流")
            }
        }

        suspend fun readFromUri(ctx: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val jsonStr = ctx.contentResolver.openInputStream(uri)?.use { input ->
                    IOUtils.readLines(input, StandardCharsets.UTF_8).joinToString(separator = "")
                }
                if (jsonStr == null)
                    throw RuntimeException("无法获取文件输入流")
                return@runCatching jsonStr
            }
        }

        /** 创建符号链接。会检查要成为符号链接的路径，如果已经有一个文件夹且不为符号链接且有内容，则抛出异常 */
        fun symlink(realFile: File, linkFile: File) {
            if (linkFile.exists() && !FileUtils.isSymlink(linkFile) && !linkFile.list().isNullOrEmpty()) {
                Log.e(
                    TAG,
                    "symlink: 停止创建符号链接！要成为符号链接的文件路径已经是一个文件夹，不为符号链接且内部不为空。删除该文件夹可能丢失文件。\n realFile=$realFile, linkFile=$linkFile"
                )
                return
            }
            try {
                linkFile.delete()
                linkFile.parentFile?.mkdirs()
                Os.symlink(realFile.absolutePath, linkFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /** FileUtils.writeStringToFile， 保证字符串结尾是一个换行。但是这个函数不会trimIndent */
        fun writeStringToFileWithLF(file: File, str: String, charset: Charset = StandardCharsets.UTF_8) {
            val str1 = str.takeIf { it.endsWith('\n') }?.let { it + "\n" } ?: str
            FileUtils.writeStringToFile(file, str1, charset)
        }

        /** 判断该文件是否为gz压缩包 */
        fun File.isGzip(): Boolean = checkHeaderMagic(byteArrayOf(0x1F.toByte(), 0x8B.toByte()))

        /** 判断该文件是否为xz压缩包 */
        fun File.isXz(): Boolean = checkHeaderMagic(XZ.HEADER_MAGIC)

        /** 检查文件头是否为给定标识 */
        private fun File.checkHeaderMagic(header: ByteArray): Boolean {
            try {
                val len = header.size.toLong()
                val fileHeader = source().use { source -> Buffer().also { source.read(it, len) }.readByteArray(len) }
                for (i in 0 until len.toInt())
                    if (header[i] != fileHeader[i])
                        return false
                return true
            } catch (e: Exception) {
                return false
            }
        }

    }

    object Rootfs {
        /**
         * 将某一个rootfs激活为当前rootfs（之后可通过rootfsCurrDir 获取)
         * 会将该rootfs文件名保存到datastore
         */
        suspend fun makeCurrent(rootfsDir: File) {
            Files.symlink(rootfsDir, rootfsCurrDir)
            dataStore.edit { it[curr_rootfs_name.key] = rootfsDir.name }
        }

        /**
         * 如果rootfs文件夹下为空或仅有current文件夹，则返回true,此时应该先提醒用户选择一个rootfs
         */
        fun haveNoRootfs(): Boolean {
            val currName = rootfsCurrDir.name
            return !(rootfsAllDir.listFiles()?.any { it.name != currName } ?: false)

        }

        private data class FileEntry(
            var name: String, val fullPath: String, val id: Long = nextId(),
            val subs: MutableSet<FileEntry> = mutableSetOf()
        ) {
            companion object {
                private var currentId = 0L
                private fun nextId(): Long = currentId++
            }

            override fun equals(other: Any?): Boolean = (this === other || (other is FileEntry && other.id == id))
            override fun hashCode(): Int = id.hashCode()
            fun add(subName: String, fullPath: String): FileEntry {
                var sub = subs.find { it.name == subName }
                if (sub == null) {
                    sub = FileEntry(subName, fullPath)
                    subs.add(sub)
                }
                return sub
            }
        }

        /**
         * 解压rootfs时，为了解压到指定文件夹d后，d内直接子文件夹就是usr opt 那些，可能需要移除压缩包中文件的路径开头多于的文件夹
         * @param reportProgress 更新进度。接收参数为当前已读取的文件字节（压缩后大小）
         * @return 返回解压时，文件名需要移除的路径前缀.
         */
        private suspend fun findRootfsTarXzRemovedPrefix(input: InputStream?, reportProgress: (Long) -> Unit = {}): String =
            withContext(Dispatchers.IO) {
                if (input == null) throw IllegalArgumentException("输入流为null")
                val startTime = System.currentTimeMillis()

                /** 要移除的多余前缀路径 如果为空字符串则表示未获取到 */
                var removedPrefix = ""
                var readUncompSize = 0L

                XZCompressorInputStream(input).use { xzIn ->
                    TarArchiveInputStream(xzIn).use { tis ->
                        val maxSegmentIdx = 10
                        val segmentList = mutableListOf<MutableSet<String>>() //每个元素是一个 路径根据 / 分割出来的路径列表
                        for (i in 0 until maxSegmentIdx) segmentList.add(mutableSetOf())
                        var maxSegmentedPath = listOf<String>() // 分段最多，也就是最深的路径。等到时候找出第几个idx是rootfs了，就以这个为基准获取idx之前的部分
                        var entry: TarArchiveEntry
                        while (tis.nextEntry.also { entry = it } != null) {
                            readUncompSize += entry.size
                            reportProgress(xzIn.compressedCount)
                            val split = entry.name.trim('/').split('/')
                            if (split.size > maxSegmentedPath.size) maxSegmentedPath = split
                            for (i in split.indices) {
                                if (i >= maxSegmentIdx) break
                                segmentList[i].add(split[i])
                            }
                        }

                        val rootfsSubDirs = listOf("etc", "usr")
                        var segmentIdx = -1
                        for (i in segmentList.indices) {
                            if (segmentList[i].containsAll(rootfsSubDirs)) {
                                segmentIdx = i
                                break //停止循环时，当前segmentIdx对应的是rootfs的子目录们
                            }
                        }

                        if (segmentIdx >= 0 && maxSegmentedPath.isNotEmpty()) {
                            //不行 现在只知道第几段的那个文件夹名，但前面几段应该是哪些文件夹名不知道了
                            // 存一个分段最多的路径吧，然后根据idx获取前半部分。但是如果rootfs目录有多个上级目录，那么最深的那个也有很小几率变成非rootfs那个文件夹里的一个文件。小概率事件不管了吧
                            removedPrefix = maxSegmentedPath.subList(0, segmentIdx).joinToString("/")
                        } else {
                            throw RuntimeException("无法找到压缩包内的多余路径前缀。segmentIdx=$segmentIdx, maxSegmentedPath=$maxSegmentedPath")
                        }
                    }
                }
                //archlinux(676MB, 32800个文件）36秒 改之前38秒。。。。到底怎么才能读取更快呢？
                Log.d(
                    TAG, "findRootfsTarXzRemovedPrefix: 找到多余路径前缀：$removedPrefix " +
                            "\n 寻找rootfs多余前缀路径耗时${(System.currentTimeMillis() - startTime) / 1000F}秒"
                )

                return@withContext removedPrefix
            }


        /**
         * 解压一个tar.xz的压缩包，其内含一个rootfs, 将其解压到outDir.
         * 解压后，outDir为 [Consts.rootfsAllDir] 中的一个目录，其内部为 bin etc 这种的目录
         * 解压后会做一些处理操作，参考 [postExtractRootfs]
         * uri不是.tar.xz时会抛出异常
         * @param reporter 调用[TaskReporter.progressValue] 时传入的是某文件压缩后大小. 本函数会将[TaskReporter.totalValue] 设置为压缩文件总大小
         */
        suspend fun installTarXzRootfs(ctx: Context, uri: Uri, outDir: File, reporter: TaskReporter) = withContext(Dispatchers.IO) {
            val tmpArchiveFile = File(Consts.tmpDir, "archive-rootfs-tmp")
            val compSize = ctx.contentResolver.openFileDescriptor(uri, "r").use { it?.statSize } ?: (1024 * 1024 * 1024L)

            reporter.progress(0F)
            reporter.totalValue = compSize

            //先检测是不是gz或xz. 然后复制文件到内部目录
            val isXz = ctx.openInput(uri)?.use { it.isXz() } ?: false
            val isGz = ctx.openInput(uri)?.use { it.isGzip() } ?: false
            if (!isXz && !isGz) {
                return@withContext reporter.done(RuntimeException("该文件不是 xz 或 gz 压缩包。"))
            }

            reporter.msg(null, "(1/3) 正在将文件复制到内部存储目录...")
//            ctx.openInput(uri)?.source()?.buffer()?.use { source ->
//                tmpArchiveFile.sink().buffer().use { sink ->
//                    sink.writeAll(source)
//                }
//            }
            tmpArchiveFile.delete()
            FileUtils.copyInputStreamToFile(ctx.openInput(uri), tmpArchiveFile)
            if (!tmpArchiveFile.exists() || tmpArchiveFile.length() != compSize) {
                return@withContext reporter.done(RuntimeException("文件复制出错，无法进行解压。"))
            }

            reporter.msg(null, "(2/3) 正在读取压缩包寻找rootfs根目录...")

            //FIXME 在压缩包内读取rootfs多余前缀太费时。先解压出来再移动文件夹？
            val removedPrefix = findRootfsTarXzRemovedPrefix(
                ctx.contentResolver.openInputStream(uri),
                reportProgress = { reporter.progress(it.toFloat() / compSize) })

            reporter.msg("找到压缩包内rootfs目录: $removedPrefix", "(3/3) 正在解压...")
//            extractTarXzRootfsInternal(ctx.contentResolver.openInputStream(uri), outDir, removedPrefix, reporter)
            val prefixCount = removedPrefix.trimStart('/').length //去除前缀 没有/开头的长度
            val compressedTarInput = when {
                isXz -> XZCompressorInputStream(ctx.openInput(uri))
                isGz -> GzipCompressorInputStream(ctx.openInput(uri))
                else -> throw RuntimeException("该文件不是 xz 或 gz 压缩包。")
            }
            Archive.decompressCompressedTarStream(compressedTarInput, outDir, reporter) {
                if (prefixCount == 0) it
                else if (it.length < prefixCount) {
                    reporter.msg("压缩包中文件名长度小于prefix长度，跳过解压：文件名=\"$it\"， prefix=$removedPrefix")
                    ""
                } else it.substring(prefixCount + if (it.startsWith('/')) 1 else 0) //如果解压文件有/开头，则去除长度+1
            }


            //解压后做一些处理操作
            reporter.msg(null, "解压结束。正在做一些处理...")
            postExtractRootfs(outDir)
        }

        /** 获取当前选择的rootfs。
         * 确保：1. 不为 [rootfsCurrDir] 2. 优先读取上次设置的，如果不存在则随机选一个
         */
        suspend fun getSelectedRootfs(): File? {
            val allAvailable = rootfsAllDir.list() ?: arrayOf()
            val selectedRootfs = curr_rootfs_name.get().takeUnless { it.isEmpty() || !allAvailable.contains(it) }
                ?: allAvailable.find { it != rootfsCurrDir.name }
            return if (selectedRootfs.isNullOrEmpty()) null
            else File(rootfsAllDir, selectedRootfs).takeIf { it.exists() }
        }

        /**
         * 解压rootfs后，需要对其做一些一次性处理
         * - 修改网络相关配置文件
         */
        suspend fun postExtractRootfs(rootfsDir: File) = withContext(Dispatchers.IO) {
            //来自proot-distro。修改网络配置文件
            File(rootfsDir, "/etc/resolv.conf").run {
                delete()
                writeText(
                    """
                    nameserver 8.8.8.8
                    nameserver 8.8.4.4
                    """.trimIndent().plus("\n")
                )
            }
            File(rootfsDir, "/etc/hosts").run {
                delete()
                writeText(
                    """
                    # IPv4.
                    127.0.0.1   localhost.localdomain localhost
            
                    # IPv6.
                    ::1         localhost.localdomain localhost ip6-localhost ip6-loopback
                    fe00::0     ip6-localnet
                    ff00::0     ip6-mcastprefix
                    ff02::1     ip6-allnodes
                    ff02::2     ip6-allrouters
                    ff02::3     ip6-allhosts
                    """.trimIndent().plus("\n")
                )
            }
        }
    }

     /** 代表一个符号链接. 用于解压文件时相关处理
      * @param symlink 符号链接的路径（安卓上的路径）
      * @param pointTo 该链接指向的路径（由symlink指定，不一定指向自己app内的路径，可能相对指向自己同目录，可能以rootfs为根目录的绝对路径，当然也可能是l2s文件指向termux内路径）
      */
    private data class SymLink(val symlink: String, val pointTo: String)
    object Archive {

        /**
         * 解压一个压缩包输入流，该压缩包解压后应该是一个tar文件，然后将这个tar文件内容解压到指定目录
         * @param archiveInput 对应压缩文件的压缩器输入流，如 [XZCompressorInputStream] [GzipCompressorInputStream]
         * @param outDir 解压到的目录，解压后该文件夹下直接子文件夹应该为 usr bin etc 那些
         * @param reporter 调用[TaskReporter.progressValue] 时传入的是某文件压缩后大小. 请确保在调用此函数前将[TaskReporter.totalValue]设置为正确的值
         * @param entryNameMapper 一个映射函数，输入压缩包内文件名a，返回修改后的文件名b，最终该文件会解压到 File([outDir], b)
         */
        fun <T> decompressCompressedTarStream(
            archiveInput: T,
            outDir: File,
            reporter: TaskReporter = TaskReporter.Dummy,
            entryNameMapper: (String) -> String = { it },
        ) where T : CompressorInputStream, T : InputStreamStatistics {
            if (!outDir.exists()) outDir.mkdirs()

            val symLinkList = mutableListOf<SymLink>()

            archiveInput.use { zis ->
                TarArchiveInputStream(zis).use { tis ->
                    var entry: TarArchiveEntry
                    while (tis.nextEntry.also { entry = it } != null) {
                        reporter.progressValue(zis.compressedCount) //更新解压进度
                        val name = entryNameMapper(entry.name)
                        if (name.isEmpty())
                            continue
                        val outFile = File(outDir, name)
                        //确保父目录存在
                        outFile.parentFile?.mkdirs()
                        try {
                            //如果是目录，创建目录
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                                Os.chmod(outFile.absolutePath, entry.mode)
                            }
                            //如果是符号链接
                            else if (entry.isSymbolicLink) {
                                symLinkList.add(SymLink(outFile.absolutePath, entry.linkName))
//                                Os.symlink(entry.linkName, outFile.absolutePath) // 全部解压完再处理吧
                            }
                            //文件，解压
                            else {
                                FileOutputStream(outFile).use { os -> tis.copyTo(os) }
                                Os.chmod(outFile.absolutePath, entry.mode) //不知为何执行权限没同步过来？
                                // FileUtils.copyInputStreamToFile(tis, file); //不能用这个，会自动关闭输入流
                            }
                        } catch (e: Exception) {
                            reporter.msg("解压文件时出错：路径=${outFile.absolutePath} 。错误消息=${e.stackTraceToString()}")
                        }
                    }
                }
            }

            for (item in symLinkList) {
                try {
                    Os.symlink(item.pointTo, item.symlink)
                    //修复 proot l2s文件相关的符号链接指向路径
                    /*
                    - 如果符号链接指向的路径以.l2s开头，说明该符号链接可能是硬链接模拟，指向的路径可能是中间文件。循环一次获取硬链接模拟到中间文件的 `map<中间文件路径，List<硬链接模拟路径>>`
                    - 如果中间文件是符号链接且指向的路径与自己同目录，且文件名只多了后缀 `.0001` 之类的数字，说明确定了中间文件和最终文件
                    - 注意，解压后的中间文件路径 不等于 硬链接模拟指向的路径，因为硬链接解压到自己包名子目录下了，检查中间文件指向的路径的时候不应该从硬链接指向的路径获取中间文件，而是应该从.l2s文件夹（或者硬链接同目录）寻找文件名相同的作为中间路径，寻找最终文件时同理
                     */
                } catch (e: Exception) {
                    reporter.msg("创建符号链接时出错。文件=$item 。错误消息=${e.stackTraceToString()}")
                }
            }
        }

        /**
         * 解压一个.tar.xz压缩文件
         * @param archiveInput 对应压缩文件的输入流
         * @param outDir 解压到的目录，解压后该文件夹下直接子文件夹应该为 usr bin etc 那些
         * @param reporter 调用[TaskReporter.progressValue] 时传入的是某文件压缩后大小. 请确保在调用此函数前将[TaskReporter.totalValue]设置为正确的值
         * @param entryNameMapper 一个映射函数，输入压缩包内文件名a，返回修改后的文件名b，最终该文件会解压到 File([outDir], b)
         */
        @Throws(IOException::class)
        fun decompressTarXz(
            archiveInput: InputStream?,
            outDir: File,
            reporter: TaskReporter = TaskReporter.Dummy,
            entryNameMapper: (String) -> String = { it },
        ) {
            XZCompressorInputStream(archiveInput).use { decompressCompressedTarStream(it, outDir, reporter, entryNameMapper) }
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
        fun <T> ViewModel.stateInSimple(initValue: T, flow: Flow<T>): StateFlow<T> {
            return flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initValue)
        }

        /**
         * 用于viewmodel中修改datastore的数据
         */
        suspend fun <T> ViewModel.editDateStore(key: Preferences.Key<T>, value: T) = withContext(Dispatchers.IO) {
            dataStore.edit { it[key] = value }
        }

        /** 同[ViewModel.editDateStore]，但会在新的协程中异步执行， */
        fun <T> ViewModel.editDateStoreAsync(key: Preferences.Key<T>, value: T) {
            viewModelScope.launch(Dispatchers.IO) { dataStore.edit { it[key] = value } }
        }
    }

    object Permissions {
        lateinit var storageLauncher: ActivityResultLauncher<String>
        fun registerForActivityResult(a: MainEmuActivity) {
            storageLauncher = a.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    CoroutineScope(Dispatchers.Default).launch {
                        a.viewModel.showConfirmDialog("未获取存储权限!")
                        requestStoragePermission()
                    }
                } else {
                    //获取权限后 启动模拟器
                    MainEmuActivity.instance.prepareAndStart()
                }
            }
        }

        /** 检查是否有存储权限。如果没有则申请。返回当前是否有权限 */
        fun checkStoragePermission(a: MainEmuActivity): Boolean {
            val granted = a.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (!granted) requestStoragePermission()
            return granted
        }

        private fun requestStoragePermission() {
            storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    object Pref {
        private const val TAG = "Utils.Pref"

        /**
         * 接收一个存储用户偏好的map,将其序列化为json
         */
        fun serializeFromMapToJson(map: Map<String, Any>): String {
            return kotlin.runCatching {
                val mapSerializer = MapSerializer(String.serializer(), PrefValueSerializer)
                return@runCatching Json.encodeToString(mapSerializer, map)
            }.onFailure { Log.e(TAG, "map转json失败", it) }.getOrNull() ?: ""

        }

        /**
         * 接收一个json字符串，将其转为map返回。map的key是datastore中对应的Key, value是对应的值
         */
        fun deserializeFromJsonToMap(json: String): Map<String, Any> {
            val _json = json.trim()
            if (_json.isEmpty()) return mapOf()
            return kotlin.runCatching {
                val mapSerializer = MapSerializer(String.serializer(), PrefValueSerializer)
                return@runCatching Json.decodeFromString<Map<String, Any>>(mapSerializer, _json)
            }.onFailure { Log.e(TAG, "获取assets/preferences.json失败\njson:$_json", it) }.getOrNull() ?: mapOf()
        }

        /**
         * 用于序列化/反序列化 偏好数据 -> json。虽说是Any 但是只处理datastore可以存的那几个类型
         */
        private object PrefValueSerializer : KSerializer<Any> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

            private val listSerializer = ListSerializer(String.serializer())

            override fun serialize(encoder: Encoder, value: Any) {
                when (value) {
                    is Boolean -> encoder.encodeBoolean(value)
                    is String -> encoder.encodeString(value)
                    is Int -> encoder.encodeInt(value)
                    is Float -> encoder.encodeFloat(value)
                    is Long -> encoder.encodeLong(value)
                    is Double -> encoder.encodeDouble(value)
                    is Set<*> -> {
                        encoder.encodeSerializableValue(listSerializer, value.map { it as String })
//                        if (value.first()?.takeIf { it is String } != null) encoder.encodeSerializableValue(setSerializer, value as Set<String>)
                    }

                    else -> throw IllegalArgumentException("序列化时，Any无法转为常见类型: ${value::class}")
                }
            }

            override fun deserialize(decoder: Decoder): Any {
                return when (val el = (decoder as JsonDecoder).decodeJsonElement()) {
                    is JsonPrimitive -> {
                        when {
                            el.isString -> el.content
                            el.booleanOrNull is Boolean -> el.content.toBoolean()
                            el.intOrNull is Int -> el.content.toInt()
                            el.floatOrNull is Float -> el.content.toFloat()
                            el.longOrNull is Long -> el.content.toLong()
                            el.doubleOrNull is Double -> el.content.toDouble()
                            else -> el.content
                        }
                    }
                    //这个数组每个元素是JsonLiteral(JsonPrimitive) 不是直接String
                    is JsonArray -> {
                        el.mapNotNull { item -> (item as JsonPrimitive).takeIf { it.isString }!!.content }.toSet()
                    }

                    else -> throw IllegalArgumentException("反序列化时，Any无法转为常见类型: $el")
                }
            }
        }
    }

    /** 当一个执行一个长时间操作时，传入一个此类的时候一遍在屏幕上显示进度和消息
     * @param totalValue 计算百分比时的分母
     */
    abstract class TaskReporter(var totalValue: Long) {

        /** 更新进度 */
        abstract fun progress(percent: Float)

        /** 和proress不同，传入参数不是 当前值/总值，而仅仅是 当前值。因为有时候调用环境不知道总值。 */
        fun progressValue(value: Long) = progress(value.toFloat() / totalValue)

        /** 执行此函数表示任务结束. 若 [error] 不为null, 说明失败了。 */
        abstract fun done(error: Exception? = null)

        /** 需要显示的文字. 当本次[title]为null时 应该显示上一次不为null的title. */
        abstract fun msg(text: String? = null, title: String? = null)

        companion object {
            val Dummy: TaskReporter = object : TaskReporter(Long.MAX_VALUE) {
                override fun progress(percent: Float) {}
                override fun done(error: Exception?) {}
                override fun msg(text: String?, title: String?) {}
            }
        }
    }
}


class RateLimiter(val delayMs: Long = 1000L) {
    private val lastBlock = AtomicReference<(suspend () -> Unit)?>(null)
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * 延迟一段时间后执行一段代码。
     * 如果这段时间只内有新代码块，则之前的代码块不会被执行，且重新开始倒计时。
     * 请在同一线程内调用
     */
    fun runDelay(block: suspend () -> Unit) {
        lastBlock.set(block)
        scope.launch {
            delay(delayMs)
            if (lastBlock.get() == block) //最后一次设置之后，过了一秒没改过
                block()
        }
    }
}


enum class FuncOnChangeAction {
    EDIT,
    ADD,
    DEL,
}
/** 增删改的回调 [FuncOnChange]的同步函数版本 */
typealias FuncOnChangeSync<T> = (oldValue: T, newValue: T, action: FuncOnChangeAction) -> Unit
/** 增删改的回调 异步函数。当为ADD或DEL时old=new */
typealias FuncOnChange<T> = suspend (oldValue: T, newValue: T, action: FuncOnChangeAction) -> Unit

