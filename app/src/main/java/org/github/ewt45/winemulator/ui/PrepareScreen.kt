package org.github.ewt45.winemulator.ui


import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.FuncOnChangeAction
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.Utils
import org.github.ewt45.winemulator.emu.ProotRootfs
import org.github.ewt45.winemulator.permissions.RequiredPermissions
import org.github.ewt45.winemulator.ui.components.ConfirmDialog
import org.github.ewt45.winemulator.ui.components.ProgressDisplay
import org.github.ewt45.winemulator.ui.components.ProgressStage
import org.github.ewt45.winemulator.ui.components.TaskReporter
import org.github.ewt45.winemulator.ui.components.SimpleTaskReporter
import org.github.ewt45.winemulator.ui.components.rememberConfirmDialogState
import org.github.ewt45.winemulator.ui.components.rememberTaskReporter
import org.github.ewt45.winemulator.ui.setting.GeneralRootfsSelect_LoginUserSelect
import org.github.ewt45.winemulator.ui.setting.GeneralRootfsSelect_RootfsName
import org.github.ewt45.winemulator.viewmodel.PrepareViewModel
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import java.io.File

private val TAG = "PrepareScreen"

@Composable
fun PrepareScreen(prepareVm: PrepareViewModel, settingVm: SettingViewModel, navigateToMainScreen: suspend () -> Unit) {
    //初次进入时 刷新状态
    LaunchedEffect(Unit) {
        prepareVm.updateState()
    }
    PrepareScreenImpl(prepareVm, settingVm, navigateToMainScreen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareScreenImpl(prepareVm: PrepareViewModel, settingVm: SettingViewModel, navigateToMainScreen: suspend () -> Unit) {
    val state by prepareVm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val reporter = rememberTaskReporter(msgTitle = "首次启动准备中...")
    var autoExtractStarted by remember { mutableStateOf(false) } // 标记是否已经开始自动提取

    // 退出prepareScreen
    LaunchedEffect(state.isPrepareFinished) {
        if (!state.isPrepareFinished) return@LaunchedEffect
        // TODO 尚未实现 restart
        if (state.shouldRestart) MainEmuActivity.instance.finish()
        else navigateToMainScreen()

    }
    
    // 首次启动时（noRootfs），自动尝试从assets提取rootfs
    // 新建容器时（forceNoRootfs）不自动提取，让用户手动选择
    LaunchedEffect(state.skipPermissions, state.unGrantedPermissions.isEmpty()) {
        val permissionsReady = state.skipPermissions || state.unGrantedPermissions.isEmpty()
        if (permissionsReady && !autoExtractStarted && state.noRootfs && !state.forceNoRootfs) {
            autoExtractStarted = true
            reporter.msgTitle = "正在自动提取Rootfs..."
            reporter.stage = ProgressStage.PROCESSING
            reporter.progress = 0
            reporter.msg = "日志："
            
            try {
                val extractedRootfs = Utils.Rootfs.installRootfsFromAssets(ctx, reporter)
                if (extractedRootfs != null) {
                    reporter.msg("自动提取rootfs成功：${extractedRootfs.name}", "自动提取成功！\n（日志可点击展开查看）")
                    reporter.stage = ProgressStage.DONE_SUCCESS
                    
                    // 自动设置启动命令为linbox
                    settingVm.onChangeProotStartupCmd("linbox")
                    reporter.msg("已设置启动命令为: linbox")
                    
                    // 自动设置当前rootfs（直接设置符号链接，不调用onChangeRootfsSelect避免触发finish）
                    Utils.Rootfs.makeCurrent(extractedRootfs)
                    // 更新状态，让UI可以继续
                    prepareVm.onRootfsExtracted(extractedRootfs.name)
                } else {
                    // 未找到assets中的rootfs，回退到手动选择
                    reporter.msg("未在assets中找到rootfs压缩包", "请手动选择rootfs压缩包")
                    reporter.stage = ProgressStage.NOT_STARTED
                    autoExtractStarted = false
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                reporter.msg("自动提取rootfs过程中出现错误：${e.stackTraceToString()}", "自动提取失败，请手动选择rootfs压缩包。\n（日志可点击展开查看）")
                reporter.stage = ProgressStage.DONE_FAILURE
                autoExtractStarted = false
            }
            reporter.progress = 100
        }
    }

    // 准备完成 启动模拟器
    if (state.isPrepareFinished) {
        Box(Modifier.fillMaxSize()) {
            Text("正在启动模拟器...", Modifier.align(Alignment.Center))
        }
    }
    // 加载中
    else if (state.loading) {
        Box(Modifier.fillMaxSize()) {
            Text("加载中...", Modifier.align(Alignment.Center))
        }
    }
    // 显示对应内容
    else {
        val lackPermissions = !(state.skipPermissions || state.unGrantedPermissions.isEmpty())
        var isRequestingPermission by remember { mutableStateOf(false) } // 禁止重复点击授予按钮
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lackPermissions) {
                PermissionGrant(isRequestingPermission, state.unGrantedPermissions, { prepareVm.onSkipPermissions() }) { permission ->
                    if (isRequestingPermission) return@PermissionGrant
                    isRequestingPermission = true
                    Utils.Permissions.request(permission.permission) { isGranted ->
                        if (isGranted) prepareVm.onGrantedPermission(permission);
                        isRequestingPermission = false
                    }
                }
            } else if (state.noRootfs || state.forceNoRootfs) {
                // 首次启动时，如果正在自动提取，显示进度
                if (state.noRootfs && !state.forceNoRootfs && 
                    (reporter.stage == ProgressStage.PROCESSING || 
                     reporter.stage == ProgressStage.DONE_SUCCESS || 
                     reporter.stage == ProgressStage.DONE_FAILURE)) {
                    RootfsAutoExtractProgress(reporter)
                }
                // 新建容器或自动提取失败/未开始时，显示手动选择
                else if (state.forceNoRootfs || (reporter.stage == ProgressStage.NOT_STARTED && !autoExtractStarted)) {
                    RootfsSelect(
                        getAvailableUsers = { rootfs: String -> ProotRootfs.getUserInfos(File(Consts.rootfsAllDir, rootfs)).map { it.name } },
                        settingVm::onChangeRootfsLoginUser, settingVm::onChangeRootfsName,
                        initReporter = reporter,
                        onAutoExtractStart = { autoExtractStarted = true }
                    )
                } else {
                    // 等待自动提取完成
                    Box(Modifier.fillMaxSize()) {
                        Text("正在准备中...", Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

/**
 * 显示用户应该授予的权限
 * @param onRequest 用户点击“授权按钮”的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionGrant(
    isRequestingPermission: Boolean,
    permissions: List<RequiredPermissions>,
    onSkip: () -> Unit,
    onRequest: (RequiredPermissions) -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("权限") },
        actions = { TextButton(onSkip) { Text("跳过") } }
    )
    Spacer(Modifier.height(16.dp))
    Column(Modifier.padding(16.dp)) {
        Text("为确保app正常运行，请授予以下权限。或者点击“跳过”，不授予权限。")
        Spacer(Modifier.height(32.dp))
        permissions.forEach { item ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.displayName, style = MaterialTheme.typography.bodyLarge)
                    if (item.description.isNotBlank())
                        Text(item.description, Modifier.padding(top = 8.dp))
                }
                Button({ onRequest(item) }, enabled = !isRequestingPermission) { Text("授予") }
            }
            if (permissions.last() != item)
                HorizontalDivider(Modifier.padding(24.dp))
        }
    }
}


/**
 * @param getAvailableUsers 传入rootfs名，返回该rootfs可选择的用户列表
 * @param onChangeUser 参考 [SettingViewModel.onChangeRootfsLoginUser]
 * @param onRootfsNameChange 参考 [SettingViewModel.onChangeRootfsName]
 * @param initReporter 可选的初始进度报告器
 * @param onAutoExtractStart 回调函数，当用户触发自动提取时调用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootfsSelect(
    getAvailableUsers: (String) -> List<String>,
    onChangeUser: suspend (String, String) -> Unit,
    onRootfsNameChange: suspend (String, String, FuncOnChangeAction) -> Unit,
    initStage: ProgressStage = ProgressStage.NOT_STARTED,
    initRootfsName: String = "",
    initReporter: SimpleTaskReporter? = null,
    onAutoExtractStart: (() -> Unit)? = null,
) {
    val TAG = "RootfsSelectScreen"
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val reporter = initReporter ?: rememberTaskReporter(msgTitle = "缺少Rootfs。请点击按钮选择一个包含Rootfs的 .tar.xz 或 .tar.gz 压缩包。")
    var rootfsName by remember { mutableStateOf(initRootfsName) }
    var isSetCurrent by remember { mutableStateOf(true) }
    val dialogState = rememberConfirmDialogState()

    val readFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        Log.d(TAG, "RootfsSelectScreen: 尝试从contentResolver获取mimetype？${ctx.contentResolver.getType(uri)}")
        reporter.stage = ProgressStage.PROCESSING
        scope.launch {
            reporter.progress = 0
            reporter.msgTitle = "正在解压中，请等待完成。"
            reporter.msg = "日志："
            try {
                rootfsName = Utils.Rootfs.installRootfsArchive(ctx, uri, reporter).name
                reporter.msg("解压rootfs成功。", "解压成功，点击按钮将退出。请手动重启。\n（日志可点击展开查看）")
                reporter.stage = ProgressStage.DONE_SUCCESS
            } catch (e: Throwable) {
                e.printStackTrace()
                reporter.msg(
                    "解压rootfs过程中出现错误，结束。\n" + e.stackTraceToString(),
                    "解压失败。请点击按钮选择一个包含Rootfs的 .tar.xz 或 .tar.gz 压缩包。\n（日志可点击展开查看）"
                )
                reporter.stage = ProgressStage.DONE_FAILURE
            }
            reporter.progress = 100
        }
    }

    ConfirmDialog(dialogState)

    CenterAlignedTopAppBar(title = { Text("Rootfs") })
    Spacer(Modifier.height(16.dp))
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
//        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //显示标题和进度
            ProgressDisplay(reporter)

            // 需要解压时显示选择按钮
            if (reporter.stage == ProgressStage.NOT_STARTED || reporter.stage == ProgressStage.DONE_FAILURE) {
                // 如果有自动提取回调，提供自动提取选项
                if (onAutoExtractStart != null) {
                    Button({
                        onAutoExtractStart()
                        // 触发自动提取的逻辑已经在PrepareScreenImpl中处理
                    }) { Text("从App内置提取") }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button({ readFileLauncher.launch(arrayOf("application/x-xz", "application/gzip", "*/*")) })
                { Text("手动选择") }
            }
            // 解压成功后显示完成按钮
            else if (reporter.stage == ProgressStage.DONE_SUCCESS) {
                Button({
                    //TODO forceNoRootfs 是否需要设置为false？
                    scope.launch {
                        if (isSetCurrent) MainEmuActivity.instance.settingViewModel.onChangeRootfsSelect(rootfsName)
                        else MainEmuActivity.instance.finish()
                    }
                }) { Text("完成") }
            }

            // 解压成功后后的其他选项，重命名，登陆用户，下次启动该容器。
            if (reporter.stage == ProgressStage.DONE_SUCCESS && rootfsName.isNotEmpty()) {
                Log.e(TAG, "RootfsSelectScreen: 解压完成后进入这里检查可登陆用户列表。平时不会进入吧？")
                HorizontalDivider(Modifier.padding(16.dp), 2.dp)
                Text("退出之前，您还可以编辑以下内容")

                GeneralRootfsSelect_RootfsName(rootfsName, false, dialogState) { oldRootfsName, newRootfsName ->
                    onRootfsNameChange(oldRootfsName, newRootfsName, FuncOnChangeAction.EDIT)
                }

                val userList = getAvailableUsers(rootfsName)
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

/**
 * 自动提取Rootfs时的进度显示组件
 */
@Composable
private fun RootfsAutoExtractProgress(reporter: SimpleTaskReporter) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 显示标题和进度
        ProgressDisplay(reporter)
        
        // 解压成功后显示提示
        if (reporter.stage == ProgressStage.DONE_SUCCESS) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Rootfs提取成功！正在设置启动命令...", style = MaterialTheme.typography.bodyLarge)
        }
        
        // 解压失败后显示提示
        if (reporter.stage == ProgressStage.DONE_FAILURE) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("提取失败，请手动选择rootfs压缩包", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview
@Composable
fun PrepareScreenPreview(
    initLackPermissions: Boolean = true,
) {
    val isLoading = false
    val noRootfs = true
    val forceNoRootfs = false
    var unGrantedPermissions by remember {
        mutableStateOf(
            listOf<RequiredPermissions>(
                RequiredPermissions.Storage,
                RequiredPermissions.Notification
            )
        )
    }
    var skipPermissions by remember { mutableStateOf(false) }
    // 加载中
    if (isLoading) {
        Box(Modifier.fillMaxSize()) {
            Text("加载中...", Modifier.align(Alignment.Center))
        }
    }
    // 显示对应内容
    else {
        val lackPermissions = !(skipPermissions || unGrantedPermissions.isEmpty()) && initLackPermissions
        var isRequestingPermission by remember { mutableStateOf(false) } // 禁止重复点击授予按钮
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lackPermissions) {
                PermissionGrant(isRequestingPermission, unGrantedPermissions, { skipPermissions = true }) { permission ->
                    if (!isRequestingPermission) {
                        isRequestingPermission = true
                        CoroutineScope(Dispatchers.Default).launch {
                            delay(1000)
                            unGrantedPermissions = unGrantedPermissions - permission
                            isRequestingPermission = false
                        }
                    }
                }
            } else if (noRootfs || forceNoRootfs) {
                val stage = ProgressStage.DONE_SUCCESS
                RootfsSelect({ listOf("iuser", "root") }, { _, _ -> }, { _, _, _ -> "" }, stage, "rootfs-1")
            }
        }
    }
}

//@Preview(widthDp = 300, heightDp = 600)
@Composable
private fun PrepareStageScreenFinishPreview() {
    val dialogState = rememberConfirmDialogState()
    ConfirmDialog(dialogState)
    ElevatedCard(Modifier.padding(16.dp)) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rootfsName = "rootfs-1"
            Text("退出之前，您还可以编辑以下内容。。")

            Spacer(Modifier.height(16.dp))
            GeneralRootfsSelect_RootfsName("rootfs-1", false, dialogState) { _, _ -> }

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