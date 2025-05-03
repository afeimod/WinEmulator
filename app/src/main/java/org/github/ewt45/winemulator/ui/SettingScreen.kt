package org.github.ewt45.winemulator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.github.ewt45.winemulator.viewmodel.SettingViewModel

@Composable
fun SettingScreen(modifier: Modifier = Modifier) {
    val settingViewModel: SettingViewModel = viewModel()
    val proot by settingViewModel.prootState.collectAsState()
    //TODO 用LazyColumn?
    Column(modifier) {
        CollapsePanel("PRoot参数") {
            ProotNoValueOptions(
                proot.proot_bool_options,
                settingViewModel::onChangeProotBoolOptions
            )
            Spacer(Modifier.height(16.dp))
            ProotStartupCmd(proot.proot_startup_cmd, settingViewModel::onChangeProotStartupCmd)
        }
    }
}

/**
 * 一些无参数的选项
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProotNoValueOptions(
    options: Set<String>,
    onCheck: (String, Boolean) -> Unit
) {

    var optionRootId = options.contains("--root-id")
    var optionL = options.contains("-L")
    var optionLink2symlink = options.contains("--link2symlink")
    var optionKillOnExit = options.contains("--kill-on-exit")
    var optionSysvipc = options.contains("--sysvipc")
    var optionAshmemMemfd = options.contains("--ashmem-memfd")
    var optionH = options.contains("-H")
    var optionP = options.contains("-P")

    FlowRow(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ChipOption(onCheck, optionRootId, "-0,--root-id", "--root-id")
        ChipOption(onCheck, optionL, "-L")
        ChipOption(onCheck, optionLink2symlink, "-l,--link2symlink", "--link2symlink")
        ChipOption(onCheck, optionKillOnExit, "--kill-on-exit")
        ChipOption(onCheck, optionSysvipc, "--sysvipc")
        ChipOption(onCheck, optionAshmemMemfd, "--ashmem-memfd")
        ChipOption(onCheck, optionH, "-H")
        ChipOption(onCheck, optionP, "-P")
    }
}

@Composable
fun ChipOption(
    onCheck: (String, Boolean) -> Unit,
    state: Boolean,
    label: String,
    key: String = label
) {
    FilterChip(
        state,
        onClick = { onCheck(key, !state) },
        label = { Text(label) },
    )
}

@Composable
fun ProotStartupCmd(
    cmd: String,
    onChange: (String) -> Unit
) {
    //用户编辑内容时，先存到这里
    var tempValue by remember { mutableStateOf(cmd) }
    LaunchedEffect(cmd) { tempValue = cmd }
    //管理焦点，当编辑完成（点击回车/按钮）时退出焦点
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    TextField(
        label = { Text("启动后执行命令") },
        value = tempValue,
        onValueChange = { tempValue = it },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            AnimatedSizeInCenter(isFocused) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "完成",
                    modifier = Modifier.clickable {
                        onChange(tempValue)
                        focusManager.clearFocus()
                    })
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onChange(tempValue)
            focusManager.clearFocus()
        }),
        interactionSource = interactionSource
    )
}


@Composable
fun TextFieldPreference() {

}

/**
 * 折叠面板
 */
@Composable
fun CollapsePanel(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "收起" else "展开"
            )
        }
        // 展开内容
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun ExpandablePanelExample() {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CollapsePanel(title = "面板一") {
            Text(text = "这是面板一的展开内容。你可以在这里放置任何你想要显示的内容，例如更长的文本、列表、图片等等。")
        }
        HorizontalDivider()
        CollapsePanel(title = "面板二") {
            Column {
                Text(text = "这是面板二的第一行内容。")
                Text(text = "这是面板二的第二行内容。")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewExpandablePanelExample() {
//    SettingScreen()
    Column {
        CollapsePanel("PRoot参数") {
            ProotNoValueOptions(setOf(), { _, _ -> })
            Spacer(Modifier.height(8.dp))
            ProotStartupCmd("proot.proot_startup_cmd", { _ -> })
        }
    }
//    var finalCmd by remember { mutableStateOf("") }
//    ProotStartupCmd(finalCmd) {finalCmd = it.replace("\n", " ").trim().trimEnd('&').trim()}
}