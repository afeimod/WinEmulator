package org.github.ewt45.winemulator.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 可展开的悬浮菜单按钮
 * 点击主按钮展开子菜单，再次点击或点击外部收起
 */
@Composable
fun ExpandableFloatingMenu(
    modifier: Modifier = Modifier,
    parentWidth: Float,
    parentHeight: Float,
    onMainMenuClick: () -> Unit,
    onGeneralSettingsClick: () -> Unit,
    onVirtualKeysClick: () -> Unit,
    onX11SettingsClick: () -> Unit,
) {
    val density = LocalDensity.current
    val buttonSizePx = with(density) { Consts.Ui.minimizedIconSize.dp.toPx() }
    
    // 拖动阈值：超过这个距离才认为是拖动
    val dragThreshold = with(density) { 30.dp.toPx() }
    
    // 初始位置（距离左上角 48dp，垂直方向 100dp）
    val initialX = with(density) { 48.dp.toPx() }
    val initialY = with(density) { 100.dp.toPx() }
    
    // 菜单是否展开
    var isExpanded by remember { mutableStateOf(false) }
    
    // 使用 rememberSaveable 持久化位置，避免重组时重置
    var offsetX by rememberSaveable { mutableStateOf(initialX) }
    var offsetY by rememberSaveable { mutableStateOf(initialY) }
    
    // 用于跟踪是否已经开始拖动
    var hasDragged by rememberSaveable { mutableStateOf(false) }
    // 用于存储按下的起始位置
    var pressStartX by rememberSaveable { mutableFloatStateOf(0f) }
    var pressStartY by rememberSaveable { mutableFloatStateOf(0f) }
    
    // 主按钮的旋转动画
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "rotation"
    )
    
    // 当父容器尺寸变化时，确保悬浮球仍在边界内
    LaunchedEffect(parentWidth, parentHeight, buttonSizePx) {
        if (parentWidth > 0 && parentHeight > 0) {
            offsetX = offsetX.coerceIn(0f, parentWidth - buttonSizePx)
            offsetY = offsetY.coerceIn(0f, parentHeight - buttonSizePx)
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 点击外部区域收起菜单
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        isExpanded = false
                    }
            )
        }
        
        // 展开的子菜单
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), (offsetY - 260f).roundToInt()) },
            enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(200)
            ) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ),
            verticalAlignment = Alignment.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // X11显示设置
                FloatingMenuItem(
                    icon = Icons.Filled.Monitor,
                    label = "X11显示设置",
                    onClick = {
                        isExpanded = false
                        onX11SettingsClick()
                    }
                )
                
                // 虚拟按键设置
                FloatingMenuItem(
                    icon = Icons.Filled.Gamepad,
                    label = "虚拟按键设置",
                    onClick = {
                        isExpanded = false
                        onVirtualKeysClick()
                    }
                )
                
                // 一般设置
                FloatingMenuItem(
                    icon = Icons.Filled.Settings,
                    label = "一般设置",
                    onClick = {
                        isExpanded = false
                        onGeneralSettingsClick()
                    }
                )
                
                // 主菜单
                FloatingMenuItem(
                    icon = Icons.Filled.Menu,
                    label = "主菜单",
                    onClick = {
                        isExpanded = false
                        onMainMenuClick()
                    }
                )
            }
        }
        
        // 主悬浮按钮
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(Consts.Ui.minimizedIconSize.dp)
                .pointerInput(Unit) {
                    // 处理拖动手势
                    detectDragGestures(
                        onDragStart = { offset ->
                            pressStartX = offset.x
                            pressStartY = offset.y
                            hasDragged = false
                        },
                        onDragEnd = {
                            if (hasDragged) {
                                // 先将位置限制在边界内
                                offsetX = offsetX.coerceIn(0f, parentWidth - buttonSizePx)
                                offsetY = offsetY.coerceIn(0f, parentHeight - buttonSizePx)
                                
                                // 吸附到最近边缘
                                val halfWidth = buttonSizePx / 2
                                val newX = if (offsetX + halfWidth < parentWidth / 2) 0f else parentWidth - buttonSizePx
                                offsetX = newX
                                offsetY = offsetY.coerceIn(0f, parentHeight - buttonSizePx)
                            }
                            // 重置拖动状态
                            hasDragged = false
                        },
                        onDragCancel = {
                            hasDragged = false
                        },
                        onDrag = { change, dragAmount ->
                            // 计算总拖动距离
                            val totalDragX = change.position.x - pressStartX
                            val totalDragY = change.position.y - pressStartY
                            val totalDistance = abs(totalDragX) + abs(totalDragY)
                            
                            // 只有超过阈值才认为是拖动
                            if (totalDistance > dragThreshold) {
                                hasDragged = true
                            }
                            
                            // 如果在拖动模式中，更新位置
                            if (hasDragged) {
                                change.consume()
                                offsetX = offsetX + dragAmount.x
                                offsetY = offsetY + dragAmount.y
                            }
                        }
                    )
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // 点击事件：只有在没有拖动的情况下才触发
                    if (!hasDragged) {
                        isExpanded = !isExpanded
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 背景
            Box(
                modifier = Modifier
                    .size(Consts.Ui.minimizedIconSize.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (isExpanded) "收起菜单" else "展开菜单",
                    modifier = Modifier
                        .size(36.dp)
                        .rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * 悬浮菜单项
 */
@Composable
private fun FloatingMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 使用 IconButton 处理点击的悬浮球版本（兼容旧版）
 */
@Deprecated("请使用 ExpandableFloatingMenu")
@Composable
fun MiniButton2(
    modifier: Modifier = Modifier,
    parentWidth: Float,
    parentHeight: Float,
    onExpand: () -> Unit
) {
    val density = LocalDensity.current
    val buttonSizePx = with(density) { Consts.Ui.minimizedIconSize.dp.toPx() }
    
    // 拖动阈值
    val dragThreshold = with(density) { 30.dp.toPx() }
    
    // 初始位置
    val initialX = with(density) { 48.dp.toPx() }
    val initialY = with(density) { 100.dp.toPx() }
    
    var offsetX by rememberSaveable { mutableStateOf(initialX) }
    var offsetY by rememberSaveable { mutableStateOf(initialY) }
    var hasDragged by rememberSaveable { mutableStateOf(false) }
    var pressStartX by rememberSaveable { mutableFloatStateOf(0f) }
    var pressStartY by rememberSaveable { mutableFloatStateOf(0f) }
    
    LaunchedEffect(parentWidth, parentHeight, buttonSizePx) {
        if (parentWidth > 0 && parentHeight > 0) {
            offsetX = offsetX.coerceIn(0f, parentWidth - buttonSizePx)
            offsetY = offsetY.coerceIn(0f, parentHeight - buttonSizePx)
        }
    }
    
    IconButton(
        onClick = {
            if (!hasDragged) {
                onExpand()
            }
        },
        modifier = Modifier
            .size(Consts.Ui.minimizedIconSize.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        pressStartX = offset.x
                        pressStartY = offset.y
                        hasDragged = false
                    },
                    onDragEnd = {
                        if (hasDragged) {
                            offsetX = offsetX.coerceIn(0f, parentWidth - buttonSizePx)
                            offsetY = offsetY.coerceIn(0f, parentHeight - buttonSizePx)
                            
                            val halfWidth = buttonSizePx / 2
                            val newX = if (offsetX + halfWidth < parentWidth / 2) 0f else parentWidth - buttonSizePx
                            offsetX = newX
                            offsetY = offsetY.coerceIn(0f, parentHeight - buttonSizePx)
                        }
                        hasDragged = false
                    },
                    onDragCancel = {
                        hasDragged = false
                    },
                    onDrag = { change, dragAmount ->
                        val totalDragX = change.position.x - pressStartX
                        val totalDragY = change.position.y - pressStartY
                        val totalDistance = abs(totalDragX) + abs(totalDragY)
                        
                        if (totalDistance > dragThreshold) {
                            hasDragged = true
                        }
                        
                        if (hasDragged) {
                            change.consume()
                            offsetX = offsetX + dragAmount.x
                            offsetY = offsetY + dragAmount.y
                        }
                    }
                )
            }
    ) {
        Icon(
            painter = painterResource(a.io.github.ewt45.winemulator.R.drawable.ic_fullscreen),
            contentDescription = "展开",
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small
                )
                .padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
