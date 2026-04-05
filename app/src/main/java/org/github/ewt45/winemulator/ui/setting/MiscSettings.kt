package org.github.ewt45.winemulator.ui.setting

import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.Utils
import org.github.ewt45.winemulator.dataStore
import org.github.ewt45.winemulator.permissions.RequiredPermissions
import org.github.ewt45.winemulator.ui.components.CollapsePanel
import org.github.ewt45.winemulator.ui.Destination
import org.github.ewt45.winemulator.ui.components.ConfirmDialog
import org.github.ewt45.winemulator.ui.components.rememberConfirmDialogState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MiscSettings"

@Composable
fun MiscSettings(navigateTo: (Destination) -> Unit) {
    CollapsePanel("杂项")  {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckPermissions(navigateTo)
            BackupRootfs()
        }
    }
}

@Composable
private fun CheckPermissions(navigateTo: (Destination) -> Unit) {
    val dialog = rememberConfirmDialogState()
    val scope = rememberCoroutineScope()
    ConfirmDialog(dialog)

    Button({
        scope.launch {
            if (RequiredPermissions.getUnGrantedList().isNotEmpty()) {
                dataStore.edit { it[Consts.Pref.Local.skip_permissions.key] = false }
                navigateTo(Destination.Prepare)
            } else {
                dialog.showConfirm("app所需权限已经全部授予！")
            }
        }
    }) { Text("检查未授予权限") }
}

@Composable
private fun BackupRootfs() {
    val dialog = rememberConfirmDialogState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isBackingUp by remember { mutableStateOf(false) }
    ConfirmDialog(dialog)

    Button(
        onClick = {
            if (isBackingUp) return@Button
            scope.launch {
                isBackingUp = true
                try {
                    val result = backupRootfsToDownload()
                    dialog.showConfirm(result)
                } catch (e: Exception) {
                    Log.e(TAG, "备份失败", e)
                    dialog.showConfirm("备份失败: ${e.message}")
                } finally {
                    isBackingUp = false
                }
            }
        },
        enabled = !isBackingUp
    ) { 
        Text(if (isBackingUp) "备份中..." else "备份当前容器") 
    }
}

/**
 * 备份当前 rootfs 到 Download 文件夹
 * 使用 tar 命令打包（不压缩），排除容器特定文件夹(.l2s, .emuconf)
 */
private suspend fun backupRootfsToDownload(): String = withContext(Dispatchers.IO) {
    val rootfsDir = Consts.rootfsCurrDir
    
    // 检查当前 rootfs 是否存在
    if (!rootfsDir.exists()) {
        return@withContext "当前容器不存在，无法备份"
    }
    
    // 获取真实的 rootfs 目录（解析符号链接）
    val realRootfsDir = rootfsDir.canonicalFile
    val rootfsName = realRootfsDir.name
    
    // 生成带时间戳的文件名
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val backupFileName = "${rootfsName}_backup_$timestamp.tar"
    
    // Download 文件夹路径
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloadsDir.exists()) {
        downloadsDir.mkdirs()
    }
    val backupFile = File(downloadsDir, backupFileName)
    
    Log.d(TAG, "开始备份: $realRootfsDir -> $backupFile")
    
    // 使用 tar 命令打包（不压缩），排除容器特定文件夹
    // 注意：需要在 rootfs 父目录执行，以便打包时使用相对路径
    val parentDir = realRootfsDir.parentFile
    val tarCmd = listOf(
        "tar",
        "-cf",
        backupFile.absolutePath,
        "--exclude=.l2s",
        "--exclude=.emuconf",
        "-C", parentDir.absolutePath,
        rootfsName
    )
    
    Log.d(TAG, "执行命令: ${tarCmd.joinToString(" ")}")
    
    val process = ProcessBuilder(tarCmd)
        .redirectErrorStream(true)
        .start()
    
    val output = Utils.readLinesProcessOutput(process)
    val exitCode = process.waitFor()
    
    if (exitCode == 0 && backupFile.exists()) {
        val sizeMB = backupFile.length() / (1024.0 * 1024.0)
        Log.d(TAG, "备份成功: ${backupFile.absolutePath}, 大小: ${"%.2f".format(sizeMB)} MB")
        "备份成功！\n路径: ${backupFile.absolutePath}\n大小: ${"%.2f".format(sizeMB)} MB"
    } else {
        Log.e(TAG, "备份失败, exitCode: $exitCode, output: $output")
        "备份失败: $output"
    }
}

@Preview
@Composable
fun MiscSettingsPreview() {
    CollapsePanel("杂项")  {
        CheckPermissions({  })
    }
}