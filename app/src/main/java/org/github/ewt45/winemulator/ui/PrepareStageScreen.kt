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
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.FuncOnChangeAction
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.Utils
import org.github.ewt45.winemulator.emu.ProotRootfs
import org.github.ewt45.winemulator.ui.setting.GeneralRootfsSelect_LoginUserSelect
import org.github.ewt45.winemulator.ui.setting.GeneralRootfsSelect_RootfsName
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import java.io.File

@Composable
fun PrepareStageScreen() {
    val setting: SettingViewModel = viewModel()
    RootfsSelectScreen(setting::onChangeRootfsLoginUser, setting::onChangeRootfsName)
}

/**
 * @param onChangeUser 参考 [SettingViewModel.onChangeRootfsLoginUser]
 * @param onRootfsNameChange 参考 [SettingViewModel.onChangeRootfsName]
 */
@Composable
fun RootfsSelectScreen(
    onChangeUser: suspend (String, String) -> Unit,
    onRootfsNameChange: suspend (String, String, FuncOnChangeAction) -> String,
) {
    val TAG = "RootfsSelectScreen"
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var extractProgress by remember { mutableFloatStateOf(0F) }
    val displayProgress by remember { derivedStateOf { (extractProgress * 100).toInt() } }
    var isError by remember { mutableStateOf(false) }      // 完成结果是 成功还是失败。
    var isProcessing by remember { mutableStateOf(false) } // 是否正在处理选择的压缩包。
    var isFinished by remember { mutableStateOf(false) } //是否完成。解压成功后提示重启

    var processingMsgTitle by remember { mutableStateOf("") }
    var processingMsg by remember { mutableStateOf("") }
    var rootfsName by remember { mutableStateOf("") }

    val processReporter = object: Utils.TaskReporter(-1) {
        override fun progress(percent: Float) {
            extractProgress = percent
        }

        override fun done(error: Exception?) {
            if (error != null) throw error
        }

        override fun msg(text: String?, title: String?) {
            if (!text.isNullOrBlank()) processingMsg += "\n$text"
            if (title != null) processingMsgTitle = title
        }
    }

    val readFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        scope.launch {
            // 选择一个目录
            val existed = Consts.rootfsAllDir.list()
            var num = 1
            while (existed != null && existed.contains("rootfs-$num")) num++
            rootfsName = "rootfs-$num"
            extractProgress = 0F
            processingMsgTitle = ""
            processingMsg = "日志："
            isError = false
            try {
                Utils.Rootfs.installRootfsArchive(ctx, uri, File(Consts.rootfsAllDir, rootfsName), processReporter)
                processingMsgTitle = "解压成功，点击按钮将退出。请手动重启。"
                isFinished = true
            } catch (e: Throwable) {
                e.printStackTrace()
                processingMsg += "\n出现错误，请重新选择压缩包\n" + e.stackTraceToString()
                isError = true
            }
            isProcessing = false
        }
    }

    val onClickFinish = if (isFinished) {
        { MainEmuActivity.instance.finish() }
    } else {
        { readFileLauncher.launch(arrayOf("application/x-xz", "*/*")) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!isFinished) {
            Text("缺少Rootfs. 请点击按钮选择一个包含Rootfs的 .tar.xz 或 .tar.gz 压缩包。", Modifier.padding(vertical = 16.dp))
            Spacer(Modifier.height(16.dp))
        }

        if (processingMsgTitle.isNotBlank()) {
            Text(processingMsgTitle)
            Spacer(Modifier.height(16.dp))
        }

        if (isProcessing) {
            LinearProgressIndicator(progress = { displayProgress / 100F })
            Text("${displayProgress}%")
            Spacer(Modifier.height(16.dp))
        } else {
            Button(onClick = onClickFinish) { Text(if (isFinished) "完成" else "选择") }
            Spacer(Modifier.height(16.dp))
        }

        if (isFinished && !isError && rootfsName.isNotEmpty()) {
            Log.e(TAG, "RootfsSelectScreen: 解压完成后进入这里检查可登陆用户列表。平时不会进入吧？")

            Text("退出之前，您还可以编辑以下内容")
            Spacer(Modifier.height(16.dp))
            GeneralRootfsSelect_RootfsName(rootfsName, false) { oldRootfsName, newRootfsName, _ ->
                onRootfsNameChange(oldRootfsName, newRootfsName, FuncOnChangeAction.EDIT)
            }

            val userList = ProotRootfs.getUserInfos(File(Consts.rootfsAllDir, rootfsName)).map { it.name }
            val nonRootUser = userList.find { it != "root" }
            if (nonRootUser != null) {
                var userName by remember { mutableStateOf(nonRootUser) }
                Spacer(Modifier.height(16.dp))
                GeneralRootfsSelect_LoginUserSelect(rootfsName, userName, userList) { rootfsName, newUserName ->
                    userName = newUserName
                    scope.launch { onChangeUser(rootfsName, newUserName) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 解压过程中的输出信息
        HorizontalDivider(Modifier.padding(vertical = 24.dp))
        Text(
            processingMsg, Modifier.padding(top = 32.dp)
                .horizontalScroll(rememberScrollState()),
            color = MaterialTheme.colorScheme.run { if (isError) error else onSurface },
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

    }
}

@Preview(widthDp = 300, heightDp = 600)
@Composable
fun PrepareStageScreenFinishPreview() {
    Column(Modifier.padding(16.dp)) {
        val rootfsName = "rootfs-1"
        Text("退出之前，您还可以编辑以下内容。。")
        Spacer(Modifier.height(16.dp))
        GeneralRootfsSelect_RootfsName("rootfs-1", false) { oldRootfsName, newRootfsName, _ -> "" }

        val userList = listOf("root", "aid_u0_a287", "iuser").filter { !it.startsWith("aid_") }.sorted()
        val nonRootUser = userList.find { it != "root" }
        if (nonRootUser != null) {
            var userName by remember { mutableStateOf(nonRootUser) }
            Spacer(Modifier.height(16.dp))
            GeneralRootfsSelect_LoginUserSelect(rootfsName, userName, userList) { _, newUserName -> userName = newUserName }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(widthDp = 300, heightDp = 600)
@Composable
fun PrepareStageScreenPreview() {
    RootfsSelectScreen({ _, _ -> }, { _, _, _ -> "" })

    Spacer(Modifier.height(32.dp))
}