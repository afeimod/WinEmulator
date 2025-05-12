package org.github.ewt45.winemulator.ui.setting

import a.io.github.ewt45.winemulator.R
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.FuncOnChange
import org.github.ewt45.winemulator.FuncOnChangeAction
import org.github.ewt45.winemulator.emu.Proot
import org.github.ewt45.winemulator.emu.ProotHelper
import org.github.ewt45.winemulator.ui.AnimatedVertical
import org.github.ewt45.winemulator.ui.CollapsePanel
import org.github.ewt45.winemulator.ui.ComposeSpinner
import org.github.ewt45.winemulator.ui.TextFieldOption
import org.github.ewt45.winemulator.ui.TitleAndContent
import org.github.ewt45.winemulator.viewmodel.MainViewModel
import org.github.ewt45.winemulator.viewmodel.PrepareStageViewModel
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel
import java.io.File
import java.util.Random

private val TAG = "GeneralSettings"


@Composable
fun GeneralSettings(
) {

    val scope = rememberCoroutineScope()

    val settingVM: SettingViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()
    val terminalViewModel: TerminalViewModel = viewModel()
    val state by settingVM.generalState.collectAsStateWithLifecycle()

    /** 当前已设置的rootfs默认登陆用户。 key为rootfs名，value为用户名  */
    val rootfsUsersCurr:Map<String, String> = Json.decodeFromString(state.localRootfsUsersCurr)

    CollapsePanel("一般选项", vPadding = 32.dp) {
        GeneralResolution(settingVM.resolutionText, settingVM::onChangeResolutionText)
        GeneralRootfsLang(state.rootfsLang, listOf("en_US.UTF-8", "zh_CN.UTF-8"), settingVM::onChangeRootfsLang)
        GeneralShareDir(state.sharedExtPath, settingVM::onChangeShareExtPath)
//        MoreContent {
        GeneralRootfsSelect(
            settingVM.rootfsList.value,
            Consts.rootfsCurrDir.canonicalFile.name,
            rootfsUsersCurr,
            settingVM.rootfsUsersOptions.value,
            settingVM::onChangeRootfsName,
            settingVM::onChangeRootfsSelect,
            {rootfs, user -> scope.launch { settingVM.onChangeRootfsLoginUser(rootfs, user, rootfsUsersCurr) }},
        )
//        }
    }
}


@Composable
fun GeneralRootfsLang(
    currLang: String,
    langOptions: List<String>,
    onLangChange: (String) -> Unit,
) {
    TitleAndContent("容器语言", "启动容器时作为环境变量 LANG 的值。") {
        ComposeSpinner(currLang, langOptions, modifier = Modifier.fillMaxWidth()) { _, new -> onLangChange(new) }
    }
}


/**
 * Rootfs切换，删除，重命名，添加
 * @param rootfsList 用于显示的rootfs名称，对应文件夹名。不应该包含[Consts.rootfsCurrDir]
 * @param currRootfs 当前正在运行的rootfs名，rootfsList中的一项，为 [Consts.rootfsCurrDir] 指向的真实路径，应该将此项禁用禁止编辑
 * @param onRootfsChange 文件夹重命名/删除时
 * @param loginUsersCurr 每个rootfs及其当前选择的登陆用户名。. 参考：[Consts.Pref.Local.rootfs_login_user_json]
 * @param loginUsersOptions 每个rootfs及其对应的全部可使用用户名
 * @param onRootfsSelectChange 当前使用的rootfs变更时
 * @param onUserSelectChange 某个rootfs的登陆用户变化时。参数1是rootfs名，参数2是用户名
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralRootfsSelect(
    rootfsList: List<String>,
    currRootfs: String,
    loginUsersCurr: Map<String, String>,
    loginUsersOptions: Map<String, List<String>>,
    onRootfsChange: suspend (String, String, FuncOnChangeAction) -> String,
    onRootfsSelectChange: suspend (String) -> Unit,
    onUserSelectChange: (String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val mainVm: MainViewModel = viewModel()
    val prepareVm: PrepareStageViewModel = viewModel()
//    TODO 排一下序之后没问题了，之前重命名用list.minus.plus 然后重命名之后rootfs名往上挪了一位，user名还没变。出错原理是什么？
    val sortedRootfsList = rootfsList.sortedWith(compareBy<String> { it != currRootfs }.thenBy { it })
//    Log.d(TAG, "GeneralRootfsSelect: compose中，列表排序后为 sortedRootfsList=$sortedRootfsList, loginUsersCurr=$loginUsersCurr")

    fun onDoneRootfsName(oldName:String, newNameRaw: String, isCurr: Boolean) {
        val newName = newNameRaw.replace(" ", "").trim()
        if (newName.isEmpty()) return
        scope.launch {
            val extraTip = if (isCurr) "\n\n该Rootfs当前正在使用，重命名后会退出app，请手动重启。" else ""
            if (mainVm.showConfirmDialog("是否将该Rootfs重命名为 $newName？$extraTip").getOrNull() == true) {
                mainVm.showBlockDialogWithErrorConfirm("正在重命名...") {
                    val result = onRootfsChange(oldName, newName, FuncOnChangeAction.EDIT)
                    if (isCurr) onRootfsSelectChange(newName)//如果是当前的，保存名称
                    return@showBlockDialogWithErrorConfirm result
                }
            }
        }
    }

    TitleAndContent("Rootfs切换", "切换Proot使用的rootfs，添加/重命名/删除。修改后需要重启app生效。") {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //添加
            FilledTonalIconButton(onClick = { prepareVm.setNoRootfs(true) }) { Icon(Icons.Filled.Add, null) }

            for (rootfsName in sortedRootfsList) {
                val isCurr = rootfsName == currRootfs
                val userNameOptions = loginUsersOptions[rootfsName] ?: listOf("root")
                val fallbackUserName = userNameOptions.find { it != "root" } ?: "root"
                val oldStoreUserName = loginUsersCurr[rootfsName]
                //从本地存储的json读取的记录的user名，可能未记录为null,也可能已记录但过时（目前没有这个用户）。如果过时就更新
                if (oldStoreUserName != null && !userNameOptions.contains(oldStoreUserName)) {
                    onUserSelectChange(oldStoreUserName, fallbackUserName)
                    continue
                }
                val userName =  oldStoreUserName ?: fallbackUserName
                Box {
                    Row(Modifier.padding(0.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1F)) {
                            TextFieldOption(rootfsName, title = "Rootfs名称", outlined = true, maxLines = 1) {
                                onDoneRootfsName(rootfsName, it, isCurr)
                            }

                            Spacer(Modifier.height(8.dp))
                            GeneralRootfsSelect_LoginUserSelect(rootfsName, userName, userNameOptions, onUserSelectChange)
                        }

                        Spacer(Modifier.width(8.dp))
                        OutlinedCard(
                            shape = RoundedCornerShape(100.dp),
                            border = CardDefaults.outlinedCardBorder(),
                        ) {
                            Column(Modifier) {
                                IconButton(onClick = {
                                    scope.launch {
                                        if (!isCurr && mainVm.showConfirmDialog("将此文件夹设置为Proot使用的rootfs？\n确定后将退出app, 请手动重启。\n\n$rootfsName").getOrNull() == true) {
                                            onRootfsSelectChange(rootfsName)
                                        }
                                    }
                                }) {
                                    if (isCurr) Icon(Icons.Filled.Check, null)
                                    else Icon(painterResource(R.drawable.ic_switch), null)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        if (mainVm.showConfirmDialog("确定删除该Rootfs吗？\n其内部所有文件都将被删除，请谨慎操作！\n\n$rootfsName").getOrNull() == true) {
                                            mainVm.showBlockDialogWithErrorConfirm("正在删除...") {
                                                onRootfsChange(rootfsName, rootfsName, FuncOnChangeAction.DEL)
                                            }
                                        }
                                    }
                                }) { Icon(Icons.Filled.Delete, null) }
                            }
                        }
                    }
                }
                if (sortedRootfsList.last() != rootfsName) {
                    HorizontalDivider(Modifier.padding(8.dp))
                }
            }
        }
    }
}

/**
 * [GeneralRootfsSelect] 的子布局。选择该rootfs的登陆用户
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralRootfsSelect_LoginUserSelect(
    rootfsName: String,
    userName: String,
    userNameOptions: List<String>,
    onUserSelectChange: (String, String) -> Unit,
) {
    ComposeSpinner(userName, userNameOptions, label = "登陆用户名", modifier=Modifier.fillMaxWidth()) { _, newValue ->
        onUserSelectChange(rootfsName, newValue)
    }
}


/**
 * 显示一个 “更多” 按钮， 点击展开更多内容
 */
@Composable
fun MoreContent(modifier: Modifier = Modifier, btnText: String = "更多...", content: @Composable AnimatedVisibilityScope.() -> Unit) {
    var isShowContent by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        AnimatedVertical(isShowContent, content = content)
        if (!isShowContent)
            TextButton(
                onClick = { isShowContent = !isShowContent },
                Modifier.align(Alignment.Center)
            ) { Text(btnText) }
    }
}

//FIXME 如果有一个共享目录是另一个共享目录的子目录，那么会无法创建符号链接
/**
 * 绑定外部共享文件夹
 * @param onPathChange 参数：路径和是否为删除
 */
@Composable
fun GeneralShareDir(
    bindSet: Set<String>,
    onPathChange: FuncOnChange<String>,//suspend (String, Boolean) -> Unit,
) {
    val mainVm: MainViewModel = viewModel()

    val scope = rememberCoroutineScope()
    val bindList = bindSet.sorted()
    val selectFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        Log.d(TAG, "GeneralShareDir: 获取到uri $uri \npath=${uri.path}")
        val path = uri.path?.split(":", limit = 2)?.get(1)
        val fullPath = if (path != null) "/storage/emulated/0/$path" else ""
        scope.launch {
            if (fullPath.isEmpty()) {
                mainVm.showConfirmDialog("添加失败！无法获取该文件夹路径。\n\nuri: $uri")
            } else if (!File(fullPath).exists()) {
                mainVm.showConfirmDialog("添加失败！该文件夹不存在。\n\npath: $fullPath \n\nuri: $uri")
            } else {
                onPathChange(fullPath, fullPath, FuncOnChangeAction.ADD)
            }
        }
    }

    TitleAndContent("共享文件夹", "在此处添加安卓上的文件夹。模拟器启动后可在容器内部访问这些文件夹。") {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //添加
            FilledTonalIconButton(onClick = { selectFolder.launch(null) }) {
                Icon(Icons.Filled.Add, null)
            }
            for (bind in bindList) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { }, verticalAlignment = Alignment.CenterVertically
                ) {
                    TextFieldOption(bind, Modifier.weight(1F), outlined = true) { newPath ->
                        scope.launch {
                            if (!File(newPath).exists())
                                mainVm.showConfirmDialog("添加失败！该文件夹不存在。\n\npath: $newPath")
                            else
                                onPathChange(bind, newPath, FuncOnChangeAction.EDIT)
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (mainVm.showConfirmDialog("确定取消该文件夹共享吗？\n\n$bind").getOrNull() == true) {
                                onPathChange(bind, bind, FuncOnChangeAction.DEL)
                            }
                        }
                    }, Modifier.size(32.dp)) { Icon(Icons.Filled.Clear, null) }
                }
            }
        }
    }

}

/** 分辨率 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralResolution(
    text: String,
    onDone: (String, Boolean) -> Unit,
) {
    val options = listOf("800x600", "1024x768", "1280x720", "1600x900", "1920x1080")
    val textInOptions = options.contains(text)
    // isCustom初始根据 分辨率是否在给定列表中 设定。后续可以手动修改用于表示用户点击了 该选项
    var isCustom by remember { mutableStateOf(!textInOptions) }
    val realText = if (isCustom) "自定义" else text
    var expanded by remember { mutableStateOf(false) }

    // 用户点击菜单项“自定义” -> 回调中设置isCustom为true -> 这种情况下不调用onDone？
    // TextField显示文字在isCustom时为 “自定义” 否则为传进来的分辨率。
    // TextField onValueChange啥也不做吧，通知viewmodel都放到点击选项时的回调里

    TitleAndContent("分辨率", "格式：宽x高，x为字母。编辑自定义分辨率后点击末尾对号图标或输入法回车保存。") {
        ExposedDropdownMenuBox(
            modifier = Modifier.fillMaxWidth(),
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
//                .focusRequester(focusRequester)
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                value = realText,
                readOnly = true,
                onValueChange = {},
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )

            val contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                //TODO 添加宽高比选择
//            Row(modifier = Modifier
//                .padding(contentPadding)
//                .horizontalScroll(ScrollState(0))
//            ) {
//                TextButton(onClick = {}) { Text("4:3") }
//                TextButton(onClick = {}) { Text("16:9") }
//                TextButton(onClick = {}) { Text("9:16") }
//            }
                DropdownMenuItem(
                    text = { Text("自定义", style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        isCustom = true
                    },
                    contentPadding = contentPadding,
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            onDone(option, false)
                            expanded = false
                            isCustom = false
                        },
                        contentPadding = contentPadding,
                    )
                }
            }
        }

        //自定义时手动输入的文本框
        AnimatedVertical(isCustom) {
            TextFieldOption(text = text, onDone = { onDone(it, true) })
        }
    }
}


@Preview(widthDp = 300, heightDp = 1000)
@Composable
fun GeneralSettingsPreview() {
    val langOptions = listOf("en_US.UTF-8", "zh_CN.UTF-8")
    var lang by remember { mutableStateOf(langOptions[0]) }
    var shareDirSet by remember { mutableStateOf(setOf("/storage/emulated/0/Download", "/storage/emulated/0/MT2")) }
    val onChangeShareDir:FuncOnChange<String> = { old, new, action ->
        if (action == FuncOnChangeAction.DEL) shareDirSet -= new
        if (action == FuncOnChangeAction.ADD) shareDirSet += "/added/path/${Random(1).nextInt()}"
        if (action == FuncOnChangeAction.EDIT) shareDirSet = shareDirSet - old + new
    }

    CollapsePanel("一般选项") {
        GeneralResolution("1280x720", { _, _ -> })
        GeneralRootfsLang(lang, langOptions, { lang = it })
        GeneralShareDir(shareDirSet, onChangeShareDir)
//        MoreContent {
        GeneralRootfsSelect(
            listOf("current", "rootfs-1", "rootfs-2"), "rootfs-1", mapOf(), mapOf(), { _, _, _ -> "" }, { _ -> }, { _, _ -> })
//        }
    }
}

