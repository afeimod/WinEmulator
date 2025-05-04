package org.github.ewt45.winemulator

import a.io.github.ewt45.winemulator.R
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.github.ewt45.winemulator.Utils.Ui.snapToNearestEdgeHalfway
import org.github.ewt45.winemulator.ui.ProotTerminalScreen
import org.github.ewt45.winemulator.ui.SettingScreen
import org.github.ewt45.winemulator.viewmodel.DialogType
import org.github.ewt45.winemulator.viewmodel.MainViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val TAG = "MainScreen"
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var minimize by remember { mutableStateOf(false) }
    var showSetting by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
//        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .then(if (minimize) Modifier.clip(RoundedCornerShape(100.dp)) else Modifier),
    ) { innerPadding ->
        //FIXME tx11已经处理键盘高度变更了，这里应该不用innerPadding 否则会有空白
        val ignoreSystemInsets = Modifier.padding(innerPadding)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier/*.padding(innerPadding)*/.fillMaxHeight()
                    .widthIn(max = 600.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!minimize) {
                        Text(
                            "占位标题",
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp)
                        )
                        SettingButton(showSetting, { showSetting = !showSetting })
                    }
                    MinimizeButton(minimize, { minimize = !minimize })
                }

                if (!minimize) {
                    if (!showSetting)
                        ProotTerminalScreen()
                    else
                        SettingScreen()
                }
            }
        }


        // 对话框
        val dialogType = uiState.dialogType
        val isConfirm = uiState.dialogType == DialogType.CONFIRM
        val isBlock = uiState.dialogType == DialogType.BLOCK
        if (dialogType != DialogType.NONE) {
            AlertDialog(
                onDismissRequest = {}, //阻止点击外部区域关闭
//                title = { Text("加载中") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth() // 让 Column 填充对话框宽度
                            .wrapContentHeight(), // 根据内容调整高度
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(uiState.msg)
                        if (isBlock) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator()
                        }
                    }
                },
                confirmButton = {
                    if (isConfirm) {
                        TextButton(onClick = { viewModel.closeConfirmDialog(true) }) { Text(stringResource(android.R.string.ok)) }
                    }
                },
                dismissButton = {
                    if (isConfirm) {
                        TextButton(onClick = { viewModel.closeConfirmDialog(false) }) { Text(stringResource(android.R.string.cancel)) }
                    }
                }
            )
        }
    }
}

/** 按钮。点击可将compose部分的视图展开或折叠。
 * 可拖动: 由于x11的acitivity是View视图，所以拖动还是要用view的layoutParam实现。
 */
@Composable
fun MinimizeButton(
    minimize: Boolean,
    onClick: () -> Unit,
) {
    val TAG = "MinimizeButton"
    val activity = LocalActivity.current
    val miniIconPx = (Consts.Ui.minimizedIconSize * LocalDensity.current.density).toInt()

    //最小化时颜色稍微变化一下吧，否则不容易看到
    val colorSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorContent = MaterialTheme.colorScheme.onSurface
    val colors =
        if (!minimize) IconButtonDefaults.iconButtonColors()
        else IconButtonColors(colorSurface, colorContent, colorSurface, colorContent)

    // 记住最小化时的位置。全屏后再次最小化时恢复到上一次位置而非默认位置
    val margin = remember { mutableListOf(0, 100) }

    IconButton(
        onClick = {
            val view = activity?.findViewById<View>(R.id.compose_view) ?: return@IconButton
            val nextValue = !minimize
            view.apply {
                val lp = layoutParams as MarginLayoutParams
                lp.height = if (nextValue) miniIconPx else MATCH_PARENT
                lp.width = if (nextValue) miniIconPx else MATCH_PARENT
                lp.leftMargin = if (nextValue) margin[0] else 0
                lp.topMargin = if (nextValue) margin[1] else 0
                lp.rightMargin = 0
                lp.bottomMargin = 0
                requestLayout()
                if (nextValue)
                    view.post { view.snapToNearestEdgeHalfway() }
            }
            onClick()
        },
        modifier = Modifier
            .size(Consts.Ui.minimizedIconSize.dp)
            .pointerInput(minimize) {
                if (!minimize)
                    return@pointerInput
                val view = activity?.findViewById<View>(R.id.compose_view) ?: return@pointerInput
                detectDragGestures(
                    onDragEnd = { view.snapToNearestEdgeHalfway() }
                ) { change, dragAmount ->
                    change.consume() //TODO 这个需要吗
                    val lp = view.layoutParams as MarginLayoutParams
                    lp.leftMargin += dragAmount.x.toInt()
                    lp.topMargin += dragAmount.y.toInt()
                    margin[0] = lp.leftMargin
                    margin[1] = lp.topMargin
                    view.requestLayout()
                }
            },
        colors = colors
    ) {
        Icon(
            painter = painterResource(if (minimize) R.drawable.ic_fullscreen else R.drawable.ic_hide),
            contentDescription = "全屏/最小化",
        )
    }
}

/**
 * 按钮，点击可显示设置界面
 */
@Composable
fun SettingButton(show: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (!show) Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "设置",
        )
        else Icon(
            painter = painterResource(R.drawable.ic_layout),
            contentDescription = "主屏幕",
        )
    }
}


@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
//    MainTheme {
//        MainScreen()
//        val terminalViewModel: TerminalViewModel = viewModel()
//        val mainViewModel: MainViewModel = viewModel()
//        LaunchedEffect(Unit) {
//            terminalViewModel.output.add(
//                "fsdfsdfsdfsdfsd" +
//                        "fsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsd11111" +
//                        "1\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\\n\n\n\n\n\n\n\n\n\n\n\nsdfsdfsdfsdfsdfsd\n\n\n\n\n\n\n\n\n\n\n\n\\n\n\n\\n\n\n\n\n\n\\n\n\n\n\n\n\n\n\n\n\nfsdfsdfsdfsdfsdfsd"
//            )
//
////            mainViewModel.showBlockDialog("测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框")
//        }
//    }
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val content = MaterialTheme.colorScheme.onSurface
    IconButton(
        onClick = {},
        colors = IconButtonColors(surface, content, surface, content)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fullscreen),
            contentDescription = "全屏/最小化",
        )
    }
}

