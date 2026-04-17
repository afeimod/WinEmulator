package org.github.ewt45.winemulator.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.github.ewt45.winemulator.Consts
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 可展开的悬浮菜单按钮
 * 点击主按钮展开子菜单，子菜单以半圆形排列显示在主按钮上方
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
    val miniButtonSizePx = with(density) { 40.dp.toPx() }
    
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
    
    // 半圆形排列参数
    val arcRadius = with(density) { 120.dp.toPx() } // 弧线半径
    val arcSpreadAngle = 180f // 半圆跨越的角度
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 点击外部区域收起菜单
        if (isExpanded) {
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
        
        // 展开的子菜单 - 半圆形排列
        if (isExpanded) {
            // 菜单项数据：图标、描述、点击回调
            val menuItems = listOf(
                Triple(Icons.Default.Home, "主菜单", onMainMenuClick),
                Triple(Icons.Filled.Settings, "一般设置", onGeneralSettingsClick),
                Triple(Icons.Default.Menu, "虚拟按键设置", onVirtualKeysClick),
                Triple(Icons.Default.Info, "X11显示设置", onX11SettingsClick)
            )
            
            // 计算半圆弧上的位置
            val centerX = offsetX + buttonSizePx / 2 - miniButtonSizePx / 2
            val centerY = offsetY - arcRadius
            
            menuItems.forEachIndexed { index, (icon, description, onClick) ->
                // 计算在弧线上的角度
                // 从左到右均匀分布
                val angleRad = PI.toFloat() * (index.toFloat() / (menuItems.size - 1))
                val angleDeg = 180f * (index.toFloat() / (menuItems.size - 1))
                
                // 计算位置
                val x = centerX + arcRadius * cos(angleRad)
                val y = centerY + arcRadius * sin(angleRad)
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isExpanded = false
                            onClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        
        // 主悬浮按钮（放在最上层）
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(Consts.Ui.minimizedIconSize.dp)
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
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!hasDragged) {
                        isExpanded = !isExpanded
                    }
                },
            contentAlignment = Alignment.Center
        ) {
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
