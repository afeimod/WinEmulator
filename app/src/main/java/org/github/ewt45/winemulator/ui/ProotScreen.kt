package org.github.ewt45.winemulator.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.github.ewt45.winemulator.ui.theme.*
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

/**
 * PRoot终端界面 - 现代化设计
 * 基于截图风格的UI：深色终端 + 浅色键盘
 */
@Composable
fun ProotTerminalScreen(viewModel: TerminalViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部状态栏 - 显示在线状态
        TerminalStatusBar(
            currentUser = viewModel.currentUser,
            currentHost = viewModel.currentHost,
            currentPath = viewModel.currentPath,
            isConnected = viewModel.isConnected
        )
        
        // 终端输出和输入区域
        ProotTerminalContent(
            viewModel = viewModel,
            onRunCommand = { viewModel.runCommand(it) }
        )
    }
}

/**
 * 终端状态栏 - 显示连接状态和基本信息
 */
@Composable
fun TerminalStatusBar(
    currentUser: String,
    currentHost: String,
    currentPath: String,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 在线状态指示器
        StatusIndicator(isConnected = isConnected)
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 用户@主机:路径 格式
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TerminalUserGreen, fontWeight = FontWeight.Bold)) {
                    append(currentUser)
                }
                withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                    append("@")
                }
                withStyle(SpanStyle(color = TerminalHostCyan, fontWeight = FontWeight.Bold)) {
                    append(currentHost)
                }
                withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                    append(":")
                }
                withStyle(SpanStyle(color = TerminalPathWhite)) {
                    append(currentPath)
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
}

/**
 * 简化版状态栏（用于预览）
 */
@Composable
fun SimpleTerminalStatusBar(
    currentUser: String,
    currentHost: String,
    currentPath: String,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusIndicator(isConnected = isConnected)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TerminalUserGreen, fontWeight = FontWeight.Bold)) {
                    append(currentUser)
                }
                withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                    append("@")
                }
                withStyle(SpanStyle(color = TerminalHostCyan, fontWeight = FontWeight.Bold)) {
                    append(currentHost)
                }
                withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                    append(":")
                }
                withStyle(SpanStyle(color = TerminalPathWhite)) {
                    append(currentPath)
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
}

/**
 * 在线状态指示器
 */
@Composable
fun StatusIndicator(isConnected: Boolean) {
    val statusColor by animateColorAsState(
        targetValue = if (isConnected) TerminalOnlineGreen else TerminalOfflineRed,
        animationSpec = tween(300),
        label = "statusColor"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isConnected) "ONLINE" else "OFFLINE",
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * 终端内容区域 - 输出和输入
 * 支持键盘弹出时自动调整布局
 */
@Composable
fun ProotTerminalContent(
    viewModel: TerminalViewModel,
    onRunCommand: (String) -> Unit
) {
    val TAG = "ProotOutputScreen"
    var execCommand by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // 自动处理键盘弹出时的padding
    ) {
        // 终端输出区域 - 深色主题
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(TerminalBackground)
        ) {
            val textVScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .verticalScroll(textVScroll)
                    .horizontalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    ColoredTerminalOutput(
                        output = viewModel.output.value,
                        coloredPrompt = viewModel.getColoredPrompt()
                    )
                }
            }

            //有内容更新时自动滚动到最底部
            LaunchedEffect(viewModel.output.value) {
                textVScroll.animateScrollTo(textVScroll.maxValue)
            }
            
            // 光标提示
            CursorBlink()
        }
        
        // 命令输入区域 - 浅色键盘风格
        KeyboardInputArea(
            execCommand = execCommand,
            onCommandChange = { execCommand = it },
            onSendCommand = {
                if (execCommand.isNotBlank()) {
                    onRunCommand(execCommand)
                    execCommand = ""
                }
            }
        )
    }
}

/**
 * 光标闪烁动画
 */
@Composable
fun CursorBlink() {
    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        while (true) {
            visible = !visible
            kotlinx.coroutines.delay(530)
        }
    }
    
    if (visible) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 8.dp)
                .size(width = 10.dp, height = 18.dp)
                .background(TerminalCursorBlock)
        )
    }
}

/**
 * 键盘输入区域 - 浅色拟物风格
 */
@Composable
fun KeyboardInputArea(
    execCommand: String,
    onCommandChange: (String) -> Unit,
    onSendCommand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KeyboardBackground)
    ) {
        // 功能快捷键栏
        FunctionKeysBar()
        
        // 输入框区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = KeyboardKeyShadow
                )
                .background(
                    color = KeyboardSurface,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = KeyboardKeyBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp)
        ) {
            TextField(
                value = execCommand,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onCommandChange,
                placeholder = {
                    Text(
                        text = "输入命令...",
                        fontFamily = FontFamily.Monospace,
                        color = KeyboardOnSurface.copy(alpha = 0.4f)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = KeyboardOnSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                trailingIcon = {
                    IconButton(
                        onClick = onSendCommand,
                        enabled = execCommand.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (execCommand.isNotBlank()) 
                                TabIndicatorPurple 
                            else 
                                KeyboardOnSurface.copy(alpha = 0.3f)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Ascii
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendCommand() }
                )
            )
        }
        
        // 虚拟键盘区域（预留，可集成现有InputControlsView）
        VirtualKeyboardPlaceholder()
    }
}

/**
 * 功能快捷键栏
 */
@Composable
fun FunctionKeysBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FunctionKeyItem(icon = Icons.Default.ChevronRight, label = "Tab")
        FunctionKeyItem(icon = null, label = "Ctrl")
        FunctionKeyItem(icon = null, label = "Alt")
        FunctionKeyItem(icon = null, label = "Esc")
        FunctionKeyItem(icon = null, label = "↑")
        FunctionKeyItem(icon = null, label = "↓")
    }
}

/**
 * 单个功能键
 */
@Composable
fun FunctionKeyItem(
    icon: ImageVector?,
    label: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) KeyboardKeyPressed else KeyboardFunctionKey,
        animationSpec = tween(100),
        label = "keyBg"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 2.dp,
        animationSpec = tween(100),
        label = "keyElevation"
    )
    
    Box(
        modifier = Modifier
            .shadow(elevation, RoundedCornerShape(8.dp), spotColor = KeyboardKeyShadow)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, KeyboardKeyBorder, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* 处理点击 */ }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = KeyboardFunctionKeyText,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = label,
                color = KeyboardFunctionKeyText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 虚拟键盘占位区域
 * 可替换为集成现有InputControlsView
 */
@Composable
fun VirtualKeyboardPlaceholder() {
    // 这里预留位置给现有的InputControlsView或自定义键盘
    // 目前显示一个简单的提示
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "虚拟键盘区域",
            color = KeyboardOnSurface.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 简单的QWERTY键盘预览
        QWERTYKeyboardPreview()
    }
}

/**
 * QWERTY键盘预览 - 拟物风格
 */
@Composable
fun QWERTYKeyboardPreview() {
    val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 第一行左侧的空位
                if (rowIndex == 1) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
                // 第三行的Shift键
                if (rowIndex == 2) {
                    SoftKey(
                        label = "⇧",
                        modifier = Modifier.width(40.dp),
                        isSpecial = true
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                row.forEach { key ->
                    SoftKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        maxWidth = 32.dp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                
                // 第三行的删除键
                if (rowIndex == 2) {
                    Spacer(modifier = Modifier.width(4.dp))
                    SoftKey(
                        label = "⌫",
                        modifier = Modifier.width(40.dp),
                        isSpecial = true
                    )
                }
            }
        }
        
        // 底部功能键行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("123", "空格", "中/英", "符").forEach { label ->
                SoftKey(
                    label = label,
                    modifier = Modifier.weight(1f),
                    isSpecial = label in listOf("123", "中/英", "符")
                )
                if (label != "符") {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

/**
 * 软拟物风格按键
 */
@Composable
fun SoftKey(
    label: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 28.dp,
    isSpecial: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> KeyboardKeyPressed
            isSpecial -> KeyboardFunctionKey
            else -> KeyboardSurface
        },
        animationSpec = tween(80),
        label = "softKeyBg"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 1.dp,
        animationSpec = tween(80),
        label = "softKeyElevation"
    )
    
    Box(
        modifier = modifier
            .height(40.dp)
            .shadow(elevation, RoundedCornerShape(8.dp), spotColor = KeyboardKeyShadow)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isPressed) KeyboardKeyBorderDark else KeyboardKeyBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* 处理按键 */ },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSpecial) KeyboardFunctionKeyText else KeyboardOnSurface,
            fontSize = if (label.length == 1) 15.sp else 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * 带颜色的终端输出渲染
 * 识别提示符行并使用彩色样式渲染
 */
@Composable
fun ColoredTerminalOutput(
    output: List<String>,
    coloredPrompt: AnnotatedString
) {
    // 提示符的正则表达式：匹配 用户名@主机:路径$ 或 用户名@主机:路径#
    val promptRegex = Regex("""([\w]+)@([\w.]+):([/~][^\s$#]*)([#$])(\s*)?$""")
    
    // 构建带样式的输出
    val annotatedOutput = buildAnnotatedString {
        output.forEach { line ->
            var remaining = line
            while (remaining.isNotEmpty()) {
                val matchResult = promptRegex.find(remaining)
                
                if (matchResult != null && matchResult.range.first >= 0) {
                    val beforePrompt = remaining.substring(0, matchResult.range.first)
                    val userName = matchResult.groupValues[1]
                    val hostName = matchResult.groupValues[2]
                    val path = matchResult.groupValues[3]
                    val symbol = matchResult.groupValues[4]
                    val trailing = matchResult.groupValues[5]
                    
                    if (beforePrompt.isNotEmpty()) {
                        append(beforePrompt)
                    }
                    
                    // 用户名颜色：root为白色，其他为绿色
                    val userColor = if (userName == "root") TerminalRootWhite else TerminalUserGreen
                    
                    withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                        append(userName)
                    }
                    withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                        append("@")
                    }
                    withStyle(SpanStyle(color = TerminalHostCyan, fontWeight = FontWeight.Bold)) {
                        append(hostName)
                    }
                    withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                        append(":")
                    }
                    withStyle(SpanStyle(color = TerminalPathWhite)) {
                        append(path)
                    }
                    withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                        append(symbol)
                        if (trailing.isNotEmpty()) {
                            append(trailing)
                        }
                    }
                    
                    val afterPromptStart = matchResult.range.last + 1
                    if (afterPromptStart < remaining.length) {
                        remaining = remaining.substring(afterPromptStart)
                    } else {
                        remaining = ""
                    }
                } else {
                    append(remaining)
                    remaining = ""
                }
            }
            append("\n")
        }
    }
    
    Text(
        text = annotatedOutput,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp
        ),
        color = TerminalOnSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * 兼容旧版本的函数（保留用于预览等场景）
 */
@Composable
fun ProotTerminalScreenImpl(
    output: SnapshotStateList<String>,
    runCommand: (String) -> Unit,
    viewModel: TerminalViewModel? = null
) {
    val previewPrompt = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(color = TerminalRootWhite, fontWeight = FontWeight.Bold)) {
                append("root")
            }
            withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                append("@")
            }
            withStyle(SpanStyle(color = TerminalHostCyan, fontWeight = FontWeight.Bold)) {
                append("localhost")
            }
            withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                append(":")
            }
            withStyle(SpanStyle(color = TerminalPathWhite)) {
                append("~")
            }
            withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                append("# ")
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val textVScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .background(TerminalBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(textVScroll)
                .horizontalScroll(rememberScrollState())
        ) {
            SelectionContainer {
                ColoredTerminalOutput(
                    output = output.toList(),
                    coloredPrompt = previewPrompt
                )
            }
        }
        
        KeyboardInputArea(
            execCommand = "",
            onCommandChange = { },
            onSendCommand = { }
        )
    }
}

/**
 * Proot终端预览函数
 */
@Composable
fun ProotTerminalScreenPreview() {
    val output = remember { mutableStateListOf<String>(
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "  终端开始运行",
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "root@localhost:~$ ls -la",
        "total 64",
        "drwxr-xr-x  5 root root 4096 Apr  7 08:00 .",
        "drwxr-xr-x  3 root root 4096 Apr  7 08:00 ..",
        "-rw-r--r--  1 root root 4096 Apr  7 08:00 file1.txt",
        "root@localhost:~$ "
    ) }
    
    val previewPrompt = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(color = TerminalRootWhite, fontWeight = FontWeight.Bold)) {
                append("root")
            }
            withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                append("@")
            }
            withStyle(SpanStyle(color = TerminalHostCyan, fontWeight = FontWeight.Bold)) {
                append("localhost")
            }
            withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                append(":")
            }
            withStyle(SpanStyle(color = TerminalPathWhite)) {
                append("~")
            }
            withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                append("# ")
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTerminalStatusBar(
            currentUser = "root",
            currentHost = "localhost",
            currentPath = "~",
            isConnected = true
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .background(TerminalBackground)
        ) {
            val textVScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .verticalScroll(textVScroll)
                    .horizontalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    ColoredTerminalOutput(
                        output = output.toList(),
                        coloredPrompt = previewPrompt
                    )
                }
            }
        }
        
        KeyboardInputArea(
            execCommand = "",
            onCommandChange = { },
            onSendCommand = { }
        )
    }
}
