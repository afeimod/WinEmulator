package org.github.ewt45.winemulator.ui


import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
    initStage: ProgressStage = ProgressStage.NOT_STARTED,
) {
    val TAG = "RootfsSelectScreen"
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var stage by remember { mutableStateOf(initStage) }
    val progress = remember { mutableIntStateOf(0) } //0-100
    val msgTitle = remember { mutableStateOf("缺少Rootfs。请点击按钮选择一个包含Rootfs的 .tar.xz 或 .tar.gz 压缩包。") }
    val msg = remember { mutableStateOf("") }
    val reporter = Utils.TaskReporter.createTaskReporter(progress, msgTitle, msg)
    var rootfsName by remember { mutableStateOf("") }
    var isSetCurrent by remember { mutableStateOf(true) }

    val readFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        Log.d(TAG, "RootfsSelectScreen: 尝试从contentResolver获取mimetype？${ctx.contentResolver.getType(uri)}")
        stage = ProgressStage.PROCESSING
        scope.launch {
            progress.intValue = 0
            msgTitle.value = "正在解压中，请等待完成。"
            msg.value = "日志："
            try {
                rootfsName = Utils.Rootfs.installRootfsArchive(ctx, uri, reporter).name
                reporter.msg("解压rootfs成功。", "解压成功，点击按钮将退出。请手动重启。\n（日志可点击展开查看）")
                stage = ProgressStage.DONE_SUCCESS
            } catch (e: Throwable) {
                e.printStackTrace()
                reporter.msg(
                    "解压rootfs过程中出现错误，结束。\n" + e.stackTraceToString(),
                    "解压失败。请点击按钮选择一个包含Rootfs的 .tar.xz 或 .tar.gz 压缩包。\n（日志可点击展开查看）"
                )
                stage = ProgressStage.DONE_FAILURE
            }
            progress.intValue = 100
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //显示标题和进度
            ProgressDisplay(stage, progress.intValue, msgTitle.value, msg.value)

            // 需要解压时显示选择按钮
            if (stage == ProgressStage.NOT_STARTED || stage == ProgressStage.DONE_FAILURE) {
                Button({ readFileLauncher.launch(arrayOf("application/x-xz", "application/gzip", "*/*")) })
                { Text("选择") }
            }
            // 解压成功后显示完成按钮
            else if (stage == ProgressStage.DONE_SUCCESS) {
                Button({
                    scope.launch {
                        if (isSetCurrent) MainEmuActivity.instance.settingViewModel.onChangeRootfsSelect(rootfsName)
                        else MainEmuActivity.instance.finish()
                    }
                }) { Text("完成") }
            }

            // 解压成功后后的其他选项，重命名，登陆用户，下次启动该容器。
            if (stage == ProgressStage.DONE_SUCCESS && rootfsName.isNotEmpty()) {
                Log.e(TAG, "RootfsSelectScreen: 解压完成后进入这里检查可登陆用户列表。平时不会进入吧？")
                HorizontalDivider(Modifier.padding(16.dp), 2.dp)
                Text("退出之前，您还可以编辑以下内容")

                GeneralRootfsSelect_RootfsName(rootfsName, false) { oldRootfsName, newRootfsName, _ ->
                    onRootfsNameChange(oldRootfsName, newRootfsName, FuncOnChangeAction.EDIT)
                }

                val userList = ProotRootfs.getUserInfos(File(Consts.rootfsAllDir, rootfsName)).map { it.name }
                userList.find { it != "root" }?.let { nonRootUser ->
                    var userName by remember { mutableStateOf(nonRootUser) }
                    GeneralRootfsSelect_LoginUserSelect(rootfsName, userName, userList) { rootfsName, newUserName ->
                        userName = newUserName
                        scope.launch { onChangeUser(rootfsName, newUserName) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("下次启动app运行该容器")
                    Checkbox(isSetCurrent, { isSetCurrent = it })
                }
            }
        }
    }
}

//@Preview(widthDp = 300, heightDp = 600)
@Composable
fun PrepareStageScreenFinishPreview() {
    ElevatedCard(Modifier.padding(16.dp)) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rootfsName = "rootfs-1"
            Text("退出之前，您还可以编辑以下内容。。")

            Spacer(Modifier.height(16.dp))
            GeneralRootfsSelect_RootfsName("rootfs-1", false) { _, _, _ -> "" }

            val userList = listOf("root", "aid_u0_a287", "iuser").filter { !it.startsWith("aid_") }.sorted()
            val nonRootUser = userList.find { it != "root" }
            if (nonRootUser != null) {
                var userName by remember { mutableStateOf(nonRootUser) }
                Spacer(Modifier.height(16.dp))
                GeneralRootfsSelect_LoginUserSelect(rootfsName, userName, userList) { _, newUserName -> userName = newUserName }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("下次启动app运行该容器")
                Checkbox(true, {})
            }
        }
    }
}

@Preview(widthDp = 300, heightDp = 600)
@Composable
fun PrepareStageScreenPreview() {
    val stage = ProgressStage.NOT_STARTED
    RootfsSelectScreen({ _, _ -> }, { _, _, _ -> "" }, stage)

    Spacer(Modifier.height(32.dp))
}