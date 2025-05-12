package org.github.ewt45.winemulator.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.github.ewt45.winemulator.ui.setting.DebugSettings
import org.github.ewt45.winemulator.ui.setting.DebugSettingsImpl
import org.github.ewt45.winemulator.ui.setting.GeneralSettings
import org.github.ewt45.winemulator.ui.setting.GeneralSettingsPreview
import org.github.ewt45.winemulator.ui.setting.ProotSettings
import org.github.ewt45.winemulator.ui.setting.ProotSettingsPreview
import org.github.ewt45.winemulator.viewmodel.MainViewModel
import org.github.ewt45.winemulator.viewmodel.SettingAction
import org.github.ewt45.winemulator.viewmodel.SettingViewModel

@Composable
fun SettingScreen(modifier: Modifier = Modifier) {
    val TAG = "SettingScreen"
    val mainViewModel: MainViewModel = viewModel()
    val settingViewModel: SettingViewModel = viewModel()
    val scope = rememberCoroutineScope()
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TopBarActions(modifier = Modifier.align(Alignment.End))
        if (Consts.isDebug) {
            DebugSettings()
            HorizontalDivider()
        }
        GeneralSettings()
        HorizontalDivider()
        ProotSettings()
        Spacer(Modifier.height(16.dp))
    }
}



/**
 * 顶部操作按钮
 */
@Composable
fun TopBarActions(
    modifier: Modifier = Modifier,
) {
    val mainVM: MainViewModel = viewModel()
    val settingVM: SettingViewModel = viewModel()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    //导出时 保存为文件
    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val th = settingVM.exportSettings(ctx, uri).exceptionOrNull()
            val resultStr = if (th != null) "导出失败。错误信息：\n\n${th.stackTraceToString()}" else "导出成功！"
            mainVM.showConfirmDialog(resultStr)
        }

    }
    //导入时 选择文件
    val readFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val th = settingVM.importSettings(ctx, uri).exceptionOrNull()
            th?.printStackTrace()
            val resultStr = if (th != null) "导入失败。错误信息：\n\n${th.stackTraceToString()}" else "导入成功！"
            mainVM.showConfirmDialog(resultStr)
        }
    }

    val onClick: (SettingAction) -> Unit = { action ->
        scope.launch {
            when (action) {
                SettingAction.EXPORT -> {
                    if (mainVM.showConfirmDialog("将设置导出为Json文件。请选择文件保存位置。").getOrNull() == true)
                        saveFileLauncher.launch("preferences.json")
                }

                SettingAction.IMPORT -> {
                    if (mainVM.showConfirmDialog("导入本地Json文件更新设置。请选择文件所在位置。").getOrNull() == true)
                        readFileLauncher.launch(arrayOf("text/*", "application/json"))
                }

                SettingAction.RESET -> {
                    if (mainVM.showConfirmDialog("将全部选项恢复为默认。是否执行此操作？").getOrNull() == true)
                        settingVM.resetSettings()
                }
            }
        }
    }

    TopBarActions(modifier, onClick = onClick)
}

@Composable
fun TopBarActions(
    modifier: Modifier = Modifier,
    onClick: (SettingAction) -> Unit = {},
) {
    Row(modifier = modifier) {
        TextButton(onClick = { onClick(SettingAction.EXPORT) }) { Text("导出") }
        TextButton(onClick = { onClick(SettingAction.IMPORT) }) { Text("导入") }
        TextButton(onClick = { onClick(SettingAction.RESET) }) { Text("重置") }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewExpandablePanelExample() {
//    SettingScreen()
    var resolution by remember { mutableStateOf("800x600") }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TopBarActions(
            modifier = Modifier.align(Alignment.End),
            onClick = {}
        )
        if (Consts.isDebug) {
            DebugSettingsImpl()
            HorizontalDivider()
        }
        GeneralSettingsPreview()
        HorizontalDivider()
        ProotSettingsPreview()
    }

//    var finalCmd by remember { mutableStateOf("") }
//    ProotStartupCmd(finalCmd) {finalCmd = it.replace("\n", " ").trim().trimEnd('&').trim()}
}

