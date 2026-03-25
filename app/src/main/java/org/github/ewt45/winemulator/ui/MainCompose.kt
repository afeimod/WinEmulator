package org.github.ewt45.winemulator.ui

import a.io.github.ewt45.winemulator.R
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


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

    // 悬浮窗口状态
    var showTerminalFloatWindow by remember { mutableStateOf(false) }
    var showSettingsFloatWindow by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MyTopAppBar(
                currDestination,
                { navigateTo(it) },
                onTerminalClick = { showTerminalFloatWindow = !showTerminalFloatWindow },
                onSettingsClick = { showSettingsFloatWindow = !showSettingsFloatWindow },
                showTerminalFloatWindow = showTerminalFloatWindow,
                showSettingsFloatWindow = showSettingsFloatWindow
            )
        },
    ) { innerPadding ->
        // FIXME tx11已经处理键盘高度变更了，这里应该不用innerPadding 否则会有空白
        //  但是使用了scaffold的topbar之后需要应用顶部padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentAlignment = Alignment.Center,
        ) {
            NavHost(
                navController, startDest.route,
                enterTransition = { scaleIn() },
                exitTransition = { scaleOut() },
            ) {
                composable<RoutePrepare> {
                    PrepareScreen(prepareVm, settingVm) {
                        MainEmuActivity.instance.startEmu()
                        navController.navigate(Destination.ExceptX11.route) { popUpTo(Destination.Prepare.route) { inclusive = true } }
                    }
                }
                composable<RouteX11> { X11Screen(tx11Content, navigateTo) }
                navigation<RouteExceptX11>(startDestination = RouteTerminal) {
                    composable<RouteTerminal> { ProotTerminalScreen(terminalVm) }
                    composable<RouteSettings> { SettingScreen(settingVm, terminalVm, prepareVm, navigateTo) }
                }
            }
        }

        // 终端悬浮窗口
        if (showTerminalFloatWindow) {
            FloatingWindowContainer(
                title = "终端",
                onClose = { showTerminalFloatWindow = false },
                defaultWidth = 400f,
                defaultHeight = 350f,
                minWidth = 250f,
                minHeight = 200f
            ) {
                ProotTerminalScreen(terminalVm)
            }
        }

        // 设置悬浮窗口
        if (showSettingsFloatWindow) {
            FloatingWindowContainer(
                title = "设置",
                onClose = { showSettingsFloatWindow = false },
                defaultWidth = 380f,
                defaultHeight = 500f,
                minWidth = 300f,
                minHeight = 300f
            ) {
                SettingScreen(settingVm, terminalVm, prepareVm, navigateTo)
            }
        }

        MainDialog(uiState) { mainVm.closeConfirmDialog(it) }

    }
}

/**
 * Android悬浮窗口容器组件
 * 使用Card和手势检测实现可拖动、可缩放的悬浮窗口
 */
@Composable
fun FloatingWindowContainer(
    title: String,
    onClose: () -> Unit,
    defaultWidth: Float = 400f,
    defaultHeight: Float = 300f,
    minWidth: Float = 200f,
    minHeight: Float = 150f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val activity = LocalActivity.current
    
    // 窗口尺寸状态（使用Float以支持流畅的动画）
    var windowWidth by remember { mutableFloatStateOf(defaultWidth) }
    var windowHeight by remember { mutableFloatStateOf(defaultHeight) }
    
    // 窗口位置状态（屏幕坐标）
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(80f) } // 初始偏移，避开状态栏
    
    // 手势状态
    var isDragging by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }
    
    // 屏幕尺寸
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }
    
    // 获取屏幕尺寸
    LaunchedEffect(Unit) {
        val displayMetrics = view.context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels.toFloat()
        screenHeight = displayMetrics.heightPixels.toFloat()
        
        // 初始化位置到屏幕中央偏上
        offsetX = (screenWidth - defaultWidth) / 2
        offsetY = 100f
    }
    
    // 动画缩放
    val scale by animateFloatAsState(
        targetValue = if (isDragging || isResizing) 1.02f else 1f,
        label = "windowScale"
    )
    
    // 阴影深度动画
    val elevation by animateFloatAsState(
        targetValue = if (isDragging || isResizing) 16f else 8f,
        label = "elevation"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(with(density) { windowWidth.toDp() })
            .height(with(density) { windowHeight.toDp() })
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                // 边界限制
                                offsetX = offsetX.coerceIn(0f, screenWidth - windowWidth)
                                offsetY = offsetY.coerceIn(0f, screenHeight - windowHeight)
                                // 吸附到边缘
                                activity?.findViewById<View>(R.id.compose_view)?.post {
                                    view.snapToNearestEdgeHalfway()
                                }
                            },
                            onDragCancel = { isDragging = false }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidth - windowWidth)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeight - windowHeight)
                        }
                    },
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
            
            // 调整大小手柄（右下角）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 4.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isResizing = true },
                                onDragEnd = { isResizing = false },
                                onDragCancel = { isResizing = false }
                            ) { change, dragAmount ->
                                change.consume()
                                val newWidth = (windowWidth + dragAmount.x).coerceIn(minWidth, screenWidth)
                                val newHeight = (windowHeight + dragAmount.y).coerceIn(minHeight, screenHeight)
                                windowWidth = newWidth
                                windowHeight = newHeight
                            }
                        }
                ) {
                    // 绘制调整大小图标（三条横线）
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(2.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
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
            onDismissRequest = {},
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
 * 顶部的AppBar，包含终端和设置按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(
    currDestination: Destination?,
    setDestination: (Destination) -> Unit,
    onTerminalClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showTerminalFloatWindow: Boolean,
    showSettingsFloatWindow: Boolean
) {
    val selectIdx = currDestination?.let { appbarDestList.indexOf(it) }
    if (selectIdx != null && selectIdx != -1) {
        TopAppBar(
            title = {
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
                // 终端悬浮窗口按钮
                IconButton(onClick = onTerminalClick) {
                    Icon(
                        painter = painterResource(
                            if (showTerminalFloatWindow) R.drawable.ic_hide else R.drawable.ic_terminal
                        ),
                        contentDescription = "终端悬浮窗口",
                        tint = if (showTerminalFloatWindow) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                // 设置悬浮窗口按钮
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置悬浮窗口",
                        tint = if (showSettingsFloatWindow) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                // X11切换按钮
                IconButton({ setDestination(Destination.X11) }) { 
                    Icon(painterResource(R.drawable.ic_hide), "X11") 
                }
            },
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
        else IconButtonDefaults.iconButtonColors(
            containerColor = colorSurface,
            contentColor = colorContent,
            disabledContainerColor = colorSurface,
            disabledContentColor = colorContent
        )

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
                MyTopAppBar(
                    currDestination,
                    { navController.navigate(it.route) },
                    onTerminalClick = {},
                    onSettingsClick = {},
                    showTerminalFloatWindow = false,
                    showSettingsFloatWindow = false
                )
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
                    enterTransition = { scaleIn() },
                    exitTransition = { scaleOut() },
                ) {
                    composable<RoutePrepare> { PrepareScreenPreview() }
                    composable<RouteX11> { X11ScreenPreview() }
                    navigation<RouteExceptX11>(startDestination = RouteTerminal) {
                        composable<RouteTerminal> { ProotTerminalScreenPreview() }
                        composable<RouteSettings> { SettingScreenPreview() }
                    }
                }
            }
        }
    }
}
