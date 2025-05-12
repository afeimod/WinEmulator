package org.github.ewt45.winemulator.ui


import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.Consts.Pref
import org.github.ewt45.winemulator.Consts.Pref.Local.rootfs_login_user_json
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.Utils
import org.github.ewt45.winemulator.Utils.Ui.editDateStore
import org.github.ewt45.winemulator.dataStore
import org.github.ewt45.winemulator.emu.ProotRootfs
import org.github.ewt45.winemulator.ui.setting.GeneralRootfsSelect_LoginUserSelect
import java.io.File

@Composable
fun PrepareStageScreen() {

}

@Composable
fun RootfsSelectScreen(
    modifier: Modifier = Modifier,
) {
    val TAG = "RootfsSelectScreen"
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

//    var readProgress by remember { mutableFloatStateOf(0F) }
    var extractProgress by remember { mutableFloatStateOf(0F) }
//    val isStartExtract by remember { derivedStateOf { extractProgress != 0F } }
    val displayProgress by remember { derivedStateOf { (extractProgress * 100).toInt() } }
    var isError by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) } // 是否正在处理选择的压缩包。
    var isFinished by remember { mutableStateOf(false) } //是否完成。解压成功后提示重启
    var info1Text by remember { mutableStateOf("缺少Rootfs. 请点击按钮选择一个包含Rootfs的 .tar.xz 或 .tar.gz 压缩包。") }
//    var info2Text by remember { mutableStateOf("") }

    var processingMsgTitle by remember { mutableStateOf("") }
    var processingMsg by remember { mutableStateOf("") }
    var rootfsName by remember { mutableStateOf("") }

    val readFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        scope.launch {
            // 选择一个目录
            val existed = Consts.rootfsAllDir.list()
            var num = 1
            while (existed != null && existed.contains("rootfs-$num")) num++
            rootfsName = "rootfs-$num"
            try {
                extractProgress = 0F
                processingMsg = ""
                processingMsgTitle = ""
                Utils.Rootfs.installTarXzRootfs(ctx, uri, File(Consts.rootfsAllDir, rootfsName), object : Utils.TaskReporter {
                    override fun progress(percent: Float) {
                        extractProgress = percent
                    }

                    override fun done(error: Exception?) {
                        if (error != null) throw error
                    }

                    override fun msg(text: String, title: String?) {
                        processingMsg = text
                        if (title != null) processingMsgTitle = title
                    }
                })
                info1Text = "解压成功，点击按钮将退出。请手动重启。"
                isError = false
                isFinished = true
            } catch (e: Throwable) {
                e.printStackTrace()
                processingMsg = "出现错误，请重新选择压缩包\n" + e.stackTraceToString()
                isError = true
            }
            isProcessing = false
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isProcessing) {
            Text(processingMsgTitle)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = { displayProgress / 100F })
            Text("${displayProgress}%")
        } else {
            Text(info1Text)
            Spacer(Modifier.height(16.dp))
            val onClick = if (isFinished) {
                { MainEmuActivity.instance.finish() }
            } else {
                { readFileLauncher.launch(arrayOf("application/x-xz", "*/*")) }
            }
            Button(onClick = onClick) { Text(if (isFinished) "完成" else "选择") }
        }

        if (isFinished && rootfsName.isNotEmpty()) {
            Log.e(TAG, "RootfsSelectScreen: 解压完成后进入这里检查可登陆用户列表。平时不会进入吧？")
            val userList = ProotRootfs.getUserInfos(File(Consts.rootfsAllDir, rootfsName)).map { it.name }.sorted()
            val nonRootUser = userList.find { it != "root" }
            if (nonRootUser != null) {
                var userName by remember { mutableStateOf(nonRootUser) }
                Spacer(Modifier.height(16.dp))
                Text("选择默认使用的登陆用户名：")
                Spacer(Modifier.height(16.dp))
                GeneralRootfsSelect_LoginUserSelect(rootfsName, userName, userList) { rootfsName, newUserName ->
                    userName = newUserName
                    scope.launch {
                        val oldMap: Map<String, String> = Json.decodeFromString(rootfs_login_user_json.get())
                        dataStore.edit { it[rootfs_login_user_json.key] = Json.encodeToString(oldMap.plus(rootfsName to userName)) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // 解压过程中的输出信息
        Spacer(Modifier.height(16.dp))
        Text(
            processingMsg, Modifier
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            color = MaterialTheme.colorScheme.run { if (isError) error else onSurface })


    }

}

@Preview(widthDp = 300, heightDp = 300)
@Composable
fun PrepareStageScreenPreview() {
    RootfsSelectScreen()
}