package org.github.ewt45.winemulator.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.github.ewt45.winemulator.ui.theme.*
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

/**
 * PRoot终端界面 - 极简全屏终端设计
 * 深色主题 + 内置输入框 + 系统输入法 + 输入法上移动画
 */
@Composable
fun ProotTerminalScreen(viewModel: TerminalViewModel) {
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    var isInputFocused by remember { mutableStateOf(false) }
    
    // 使用键盘可见性状态
    val keyboardVisibleState = rememberKeyboardVisibility()
    var isKeyboardVisible by remember { mutableStateOf(false) }
    
    // 监听键盘可见性状态变化
    LaunchedEffect(keyboardVisibleState.value) {
        isKeyboardVisible = keyboardVisibleState.value
    }
    
    // 键盘打开时的上移动画
    val keyboardPadding by animateDpAsState(
        targetValue = if (isKeyboardVisible) 48.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "keyboardPadding"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        // 顶部状态栏 - 简洁设计
        TerminalHeaderBar(
            currentUser = viewModel.currentUser,
            currentHost = viewModel.currentHost,
            currentPath = viewModel.currentPath,
            isConnected = viewModel.isConnected
        )
        
        // 终端输出区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // 点击终端区域时聚焦输入框
                    focusRequester.requestFocus()
                }
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
            
            // 有内容更新时自动滚动到最底部
            LaunchedEffect(viewModel.output.value) {
                textVScroll.animateScrollTo(textVScroll.maxValue)
            }
        }
        
        // 内置命令输入行 - 直接集成在终端底部
        // 添加键盘上移动画效果
        Box(
            modifier = Modifier
                .offset(y = -keyboardPadding)
                .imePadding() // 确保输入法打开时不被遮挡
        ) {
            IntegratedCommandInput(
                value = inputValue,
                onValueChange = { inputValue = it },
                onSendCommand = {
                    if (inputValue.text.isNotBlank()) {
                        viewModel.runCommand(inputValue.text)
                        inputValue = TextFieldValue("")
                    }
                },
                focusRequester = focusRequester,
                onFocusChanged = { isInputFocused = it },
                isKeyboardVisible = isKeyboardVisible
            )
        }
    }
}

/**
 * 终端顶部状态栏 - 极简风格
 */
@Composable
fun TerminalHeaderBar(
    currentUser: String,
    currentHost: String,
    currentPath: String,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 在线状态点
        ConnectionIndicator(isConnected = isConnected)
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // 提示符
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TerminalUserGreen, fontWeight = FontWeight.Medium)) {
                    append(currentUser)
                }
                withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                    append("@")
                }
                withStyle(SpanStyle(color = TerminalHostCyan, fontWeight = FontWeight.Medium)) {
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
            fontSize = 12.sp
        )
    }
}

/**
 * 连接状态指示器 - 小圆点
 */
@Composable
fun ConnectionIndicator(isConnected: Boolean) {
    val statusColor by animateColorAsState(
        targetValue = if (isConnected) TerminalOnlineGreen else TerminalOfflineRed,
        animationSpec = tween(300),
        label = "statusColor"
    )
    
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(statusColor)
    )
}

/**
 * 内置命令输入行 - 直接集成在终端底部
 * 点击后使用系统输入法
 * 当输入法打开时添加视觉上移效果
 */
@Composable
fun IntegratedCommandInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSendCommand: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    isKeyboardVisible: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    // 键盘打开时增加输入区域的背景对比度
    val inputBackgroundColor by animateColorAsState(
        targetValue = if (isKeyboardVisible) TerminalSurface else TerminalSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "inputBackground"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(inputBackgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 提示符前缀（灰色）
        Text(
            text = "> ",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = TerminalSymbolYellow,
            fontWeight = FontWeight.Bold
        )
        
        // 可编辑的输入区域
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    onFocusChanged(state.isFocused)
                },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = TerminalOnSurface,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(TerminalCursor),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
                keyboardType = KeyboardType.Ascii
            ),
            keyboardActions = KeyboardActions(
                onSend = { onSendCommand() }
            ),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            text = "点击输入命令...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = TerminalOnSurface.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // 发送按钮（只有输入内容时显示）
        if (value.text.isNotEmpty()) {
            TextButton(
                onClick = onSendCommand,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "发送",
                    color = TabIndicatorPurple,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
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
    // 提示符的正则表达式
    val promptRegex = Regex("""([\w]+)@([\w.]+):([/~][^\s$#]*)([#$])(\s*)?$""")
    
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
                    
                    // 用户名颜色
                    val userColor = if (userName == "root") TerminalRootWhite else TerminalUserGreen
                    
                    withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Medium)) {
                        append(userName)
                    }
                    withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                        append("@")
                    }
                    withStyle(SpanStyle(color = TerminalHostCyan, fontWeight = FontWeight.Medium)) {
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
 * 终端预览函数
 */
@Composable
fun ProotTerminalScreenPreview() {
    val output = remember { mutableStateListOf(
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "  Linux Terminal Ready",
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "root@localhost:~$ ls -la",
        "total 64",
        "drwxr-xr-x  5 root root 4096 Apr  7 08:00 .",
        "drwxr-xr-x  3 root root 4096 Apr  7 08:00 ..",
        "-rw-r--r--  1 root root 4096 Apr  7 08:00 file1.txt",
        "root@localhost:~$ "
    ) }
    
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        TerminalHeaderBar(
            currentUser = "root",
            currentHost = "localhost",
            currentPath = "~",
            isConnected = true
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                        coloredPrompt = buildAnnotatedString { }
                    )
                }
            }
        }
        
        IntegratedCommandInput(
            value = inputValue,
            onValueChange = { inputValue = it },
            onSendCommand = {
                if (inputValue.text.isNotBlank()) {
                    output.add("root@localhost:~$ ${inputValue.text}")
                    output.add("Command executed: ${inputValue.text}")
                    inputValue = TextFieldValue("")
                }
            },
            focusRequester = focusRequester,
            onFocusChanged = { },
            isKeyboardVisible = false
        )
    }
}

/**
 * 键盘可见性状态类
 * 用于监听输入法键盘的显示/隐藏状态
 */
class KeyboardVisibilityState {
    var isKeyboardVisible by mutableStateOf(false)
        private set
    
    var keyboardHeight by mutableStateOf(0.dp)
        private set
    
    fun updateState(isVisible: Boolean, heightDp: Dp) {
        isKeyboardVisible = isVisible
        keyboardHeight = heightDp
    }
}

/**
 * 组合函数：监听键盘可见性变化
 * 使用 WindowInsets 精确检测键盘高度
 * 返回键盘可见性状态
 */
@Composable
fun rememberKeyboardVisibility(): State<Boolean> {
    val keyboardState = remember { KeyboardVisibilityState() }
    val view = LocalView.current
    
    DisposableEffect(Unit) {
        val listener = ViewCompat.OnApplyWindowInsetsListener { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // 计算实际的键盘高度（减去导航栏高度）
            val keyboardHeightPx = imeInsets.bottom - navInsets.bottom
            val density = androidx.compose.ui.platform.LocalDensity.current
            val keyboardHeightDp = with(density) { keyboardHeightPx.toDp() }
            
            // 键盘可见且高度大于阈值（100px）
            val isVisible = imeInsets.bottom > 0 && keyboardHeightPx > 100
            
            keyboardState.updateState(isVisible, maxOf(keyboardHeightDp, 0.dp))
            insets
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(view, listener)
        
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }
    
    return remember {
        derivedStateOf { keyboardState.isKeyboardVisible }
    }
}
