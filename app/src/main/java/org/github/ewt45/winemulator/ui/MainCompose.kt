package org.github.ewt45.winemulator.ui

import a.io.github.ewt45.winemulator.R
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.Utils.Ui.snapToNearestEdgeHalfway
import org.github.ewt45.winemulator.ui.theme.MainTheme
import org.github.ewt45.winemulator.viewmodel.DialogType
import org.github.ewt45.winemulator.viewmodel.MainUiState
import org.github.ewt45.winemulator.viewmodel.MainViewModel
import org.github.ewt45.winemulator.viewmodel.PrepareViewModel
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    tx11Content: (Context) -> View,
    startDest: Destination,
    mainVm: MainViewModel,
    terminalVm: TerminalViewModel,
    settingVm: SettingViewModel,
    prepareVm: PrepareViewModel,
) {
    val TAG = "MainScreen"
    val navController = rememberNavController()

    val uiState by mainVm.uiState.collectAsState()
    val prepareUiState by prepareVm.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currDestination = appbarDestList.find { navBackStackEntry?.destination?.hasRoute(it.route::class) == true } ?: startDest

    // 跳转到目的地。如果返回栈中有该目的地，则弹出到这个位置然后再跳转。
    val navigateTo: (Destination) -> Unit = { navController.navigate(it.route) { popUpTo(it.route) { inclusive = true } } }

    // acitivty通过viewmodel修改目的地时，触发跳转
    LaunchedEffect(Unit) {
        mainVm.navigateToEvent.collect { dest -> navigateTo(dest) }
    }
    // 开头或中途 需要进入准备屏幕时
    LaunchedEffect(prepareUiState.isPrepareFinished) {
        if (!prepareUiState.isPrepareFinished && currDestination != Destination.Prepare)
            navigateTo(Destination.Prepare)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MyTopAppBar(currDestination, { navigateTo(it) })
        },
    ) { innerPadding ->
        // FIXME tx11已经处理键盘高度变更了，这里应该不用innerPadding 否则会有空白
        //  但是使用了scaffold的topbar之后需要应用顶部padding
//            val ignoreSystemInsets = Modifier.padding(innerPadding)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentAlignment = Alignment.Center,
        ) {
//            Column(Modifier.fillMaxHeight().widthIn(max = 600.dp)) { }
            NavHost(
                navController, startDest.route,
                enterTransition = { androidx.compose.animation.scaleIn() },
                exitTransition = { androidx.compose.animation.scaleOut() },
            ) {
                composable<RoutePrepare> {
                    PrepareScreen(prepareVm, settingVm) {
                        MainEmuActivity.instance.startEmu()
                        navController.navigate(Destination.ExceptX11.route) { popUpTo(Destination.Prepare.route) { inclusive = true } }
                    }
                }
                composable<RouteX11> { X11Screen(tx11Content, { }) }
                navigation<RouteExceptX11>(startDestination = RouteTerminal) {
                    composable<RouteTerminal> { ProotTerminalScreen(terminalVm) }
//                        composable<NavDest.Terminal> { TerminalScreen() }
                    composable<RouteSettings> { SettingScreen(settingVm, terminalVm, prepareVm, navigateTo) }
                }
            }
        }

        MainDialog(uiState) { mainVm.closeConfirmDialog(it) }

    }
}

@Composable
private fun MainDialog(uiState: MainUiState, onClose: (Boolean) -> Unit) {
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
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SelectionContainer {
                        Text(uiState.msg, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }
                    if (isBlock) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                if (isConfirm) {
                    TextButton(onClick = { onClose(true) }) { Text(stringResource(android.R.string.ok)) }
                }
            },
            dismissButton = {
                if (isConfirm) {
                    TextButton(onClick = { onClose(false) }) { Text(stringResource(android.R.string.cancel)) }
                }
            }
        )
    }
}


/**
 * 顶部的AppBar，显示一个tabRow，包含一些导航目的地。如果传入当前目的地不在列表内，则不显示appbar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(
    currDestination: Destination?,
    setDestination: (Destination) -> Unit,
) {
    val selectIdx = currDestination?.let { appbarDestList.indexOf(it) }
    if (selectIdx != null && selectIdx != -1) {
        TopAppBar(
            title = {
                //TODO 改为 navigation  参考 https://developer.android.com/develop/ui/compose/components/tabs?hl=zh-cn
                PrimaryScrollableTabRow(selectIdx, divider = {}) {
                    appbarDestList.forEachIndexed { idx, dest ->
                        Tab(
                            selected = selectIdx == idx,
                            onClick = { setDestination(dest) },
                            text = {
                                Text(
                                    text = dest.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        )
                    }
                }
            },
            actions = {
                IconButton({ setDestination(Destination.X11) }) { Icon(painterResource(R.drawable.ic_hide), null) }
            },
//        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        )
    }

}

/** 按钮。点击可将compose部分的视图展开或折叠。
 * 可拖动: 由于x11的acitivity是View视图，所以拖动还是要用view的layoutParam实现。
 */
@Composable
private fun MinimizeButton(
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
        else androidx.compose.material3.IconButtonDefaults.iconButtonColors(containerColor = colorSurface, contentColor = colorContent)

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
                    change.consume()
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

/**
 * X11作为主界面的主屏幕
 * X11界面全屏显示，终端和设置界面以浮动窗口形式叠加在X11界面上，默认处于最小化状态
 * 关键点：隐藏面板时使用graphicsLayer(alpha=0f)隐藏可见性，但保持组件在组合树中，
 * 这样TerminalViewModel不会被清除，终端进程会继续运行
 */
@Composable
fun MainScreenWithX11AsMain(
    tx11Content: (Context) -> View,
    mainVm: MainViewModel,
    terminalVm: TerminalViewModel,
    settingVm: SettingViewModel,
    prepareVm: PrepareViewModel,
) {
    val TAG = "MainScreenWithX11AsMain"

    val uiState by mainVm.uiState.collectAsState()
    val prepareUiState by prepareVm.uiState.collectAsStateWithLifecycle()

    // 跟踪各个面板的最小化状态 - 默认为true（最小化）
    var terminalMinimized by remember { mutableStateOf(true) }
    var settingsMinimized by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // X11界面作为底层（全屏显示）
        X11Screen(
            x11Content = tx11Content,
            onNavigateToOthers = { dest: Destination ->
                when (dest) {
                    Destination.Terminal -> {
                        terminalMinimized = false
                    }
                    Destination.Settings -> {
                        settingsMinimized = false
                    }
                    else -> {}
                }
            }
        )

        // 快速访问按钮（始终显示在右上角）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxSize()
        ) {
            QuickAccessButtons(
                terminalMinimized = terminalMinimized,
                settingsMinimized = settingsMinimized,
                onTerminalClick = {
                    terminalMinimized = !terminalMinimized
                },
                onSettingsClick = {
                    settingsMinimized = !settingsMinimized
                }
            )
        }

        // 终端面板 - 始终保持在组合树中以保持终端进程运行
        TerminalFloatingPanel(
            minimized = terminalMinimized,
            onMinimize = { terminalMinimized = true },
            viewModel = terminalVm
        )

        // 设置面板 - 始终保持在组合树中
        SettingsFloatingPanel(
            minimized = settingsMinimized,
            onMinimize = { settingsMinimized = true },
            settingVm = settingVm,
            terminalVm = terminalVm,
            prepareVm = prepareVm
        )

        // 对话框
        MainDialog(uiState) { mainVm.closeConfirmDialog(it) }
    }
}

/**
 * 快速访问按钮组件
 */
@Composable
private fun QuickAccessButtons(
    terminalMinimized: Boolean,
    settingsMinimized: Boolean,
    onTerminalClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        QuickAccessButton(
            text = if (terminalMinimized) "终端" else "终端▲",
            highlighted = !terminalMinimized,
            onClick = onTerminalClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuickAccessButton(
            text = if (settingsMinimized) "设置" else "设置▲",
            highlighted = !settingsMinimized,
            onClick = onSettingsClick
        )
    }
}

/**
 * 快速访问按钮
 */
@Composable
private fun QuickAccessButton(
    text: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (highlighted) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(containerColor = buttonColor)
    ) {
        Text(text = text)
    }
}

/**
 * 终端浮动面板
 * 
 * 重要设计决策：
 * 1. 面板内容（包括ProotTerminalScreen）始终保持在组合树中
 * 2. 这样可以确保TerminalViewModel不会被清除，终端进程继续运行
 * 3. 最小化状态使用以下Modifier控制：
 *    - alpha=0f: 完全隐藏可见性（但组件仍在渲染）
 *    - pointerInput: 拦截所有触摸事件
 *    - 固定小尺寸: 不占用过多布局空间
 */
@Composable
private fun TerminalFloatingPanel(
    minimized: Boolean,
    onMinimize: () -> Unit,
    viewModel: TerminalViewModel
) {
    // 获取屏幕尺寸用于计算面板大小
    val configuration = LocalConfiguration.current
    val panelWidth = (configuration.screenWidthDp * 0.4f).dp
    val panelHeight = (configuration.screenHeightDp * 0.5f).dp
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        // 面板容器 - 始终存在，但使用Modifier控制可见性和交互
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 72.dp, start = 16.dp)
                // 关键：使用graphicsLayer和固定尺寸来隐藏，而不是size(0.dp)
                // 这样可以避免Compose布局系统的问题
                .size(width = if (minimized) 60.dp else panelWidth, height = if (minimized) 40.dp else panelHeight)
                .graphicsLayer {
                    // 最小化时完全隐藏可见性，但组件仍在渲染
                    alpha = if (minimized) 0f else 1f
                }
                .pointerInput(minimized) {
                    // 最小化时拦截所有点击事件
                    if (minimized) {
                        // 无限等待，拦截所有触摸事件
                        try {
                            while (true) {
                                // 获取一个不会完成的channel或直接等待
                                // 这里使用空循环阻塞
                            }
                        } catch (e: Throwable) {
                            // 忽略
                        }
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
            // Card容器 - 始终渲染以保持终端进程
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 标题栏 - 仅在非最小化时显示
                    AnimatedVisibility(
                        visible = !minimized,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FloatingPanelHeader(
                            title = "终端",
                            onMinimize = onMinimize,
                            onClose = onMinimize
                        )
                    }
                    
                    // 内容区 - 始终渲染！这是保持终端进程运行的关键
                    // 即使在最小化状态下，ProotTerminalScreen仍在组合树中
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (minimized) {
                                    // 最小化时也拦截内容区的点击
                                    Modifier.pointerInput(Unit) {
                                        while (true) {
                                            // 空循环，拦截所有事件
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        // 关键：ProotTerminalScreen必须始终在组合树中
                        // 这样TerminalViewModel不会被清除，终端进程继续运行
                        ProotTerminalScreen(viewModel)
                    }
                }
            }
        }
    }
}

/**
 * 设置浮动面板
 * 
 * 采用与TerminalFloatingPanel相同的设计策略：
 * 始终保持内容在组合树中，使用Modifier控制可见性和交互
 */
@Composable
private fun SettingsFloatingPanel(
    minimized: Boolean,
    onMinimize: () -> Unit,
    settingVm: SettingViewModel,
    terminalVm: TerminalViewModel,
    prepareVm: PrepareViewModel
) {
    // 获取屏幕尺寸用于计算面板大小
    val configuration = LocalConfiguration.current
    val panelWidth = (configuration.screenWidthDp * 0.4f).dp
    val panelHeight = (configuration.screenHeightDp * 0.5f).dp
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        // 面板容器 - 始终存在
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 130.dp, start = 16.dp)
                // 使用graphicsLayer控制可见性，固定小尺寸减少布局影响
                .size(width = if (minimized) 60.dp else panelWidth, height = if (minimized) 40.dp else panelHeight)
                .graphicsLayer {
                    alpha = if (minimized) 0f else 1f
                }
                .pointerInput(minimized) {
                    if (minimized) {
                        while (true) {
                            // 拦截所有事件
                        }
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 标题栏 - 仅在非最小化时显示
                    AnimatedVisibility(
                        visible = !minimized,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FloatingPanelHeader(
                            title = "设置",
                            onMinimize = onMinimize,
                            onClose = onMinimize
                        )
                    }
                    
                    // 内容区 - 始终渲染
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (minimized) {
                                    Modifier.pointerInput(Unit) {
                                        while (true) {
                                            // 拦截所有事件
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        SettingScreen(settingVm, terminalVm, prepareVm) { }
                    }
                }
            }
        }
    }
}

/**
 * 浮动面板标题栏
 */
@Composable
private fun FloatingPanelHeader(
    title: String,
    onMinimize: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_hide),
                    contentDescription = "最小化",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_small),
                    contentDescription = "关闭",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    val startDest = Destination.ExceptX11
    val navController = rememberNavController()
    val navBackEntry by navController.currentBackStackEntryAsState()
    val currDestination = appbarDestList.find { navBackEntry?.destination?.hasRoute(it.route::class) == true }
    MainTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MyTopAppBar(currDestination, { navController.navigate(it.route) })
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentAlignment = Alignment.Center,
            ) {
                NavHost(
                    navController, startDest.route,
                    enterTransition = { androidx.compose.animation.scaleIn() },
                    exitTransition = { androidx.compose.animation.scaleOut() },
                ) {
                    composable<RoutePrepare> { PrepareScreenPreview() }
                    composable<RouteX11> { X11ScreenPreview() }
                    navigation<RouteExceptX11>(startDestination = RouteTerminal) {
                        composable<RouteTerminal> { ProotTerminalScreenPreview() }
//                        composable<NavDest.Terminal> { TerminalScreenPreview() }
                        composable<RouteSettings> { SettingScreenPreview() }
                    }
                }
            }
//            MainDialog(uiState) { mainVM.closeConfirmDialog(it) }
        }
    }
}
