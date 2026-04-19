package org.github.ewt45.winemulator.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.github.ewt45.winemulator.ui.theme.*
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

/**
 * PRoot终端界面 - 极简全屏终端设计
 * 深色主题 + 内置输入框 + 系统输入法
 */
@Composable
fun ProotTerminalScreen(viewModel: TerminalViewModel) {
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    var isInputFocused by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .imePadding()
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
            onFocusChanged = { isInputFocused = it }
        )
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
 */
@Composable
fun IntegratedCommandInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSendCommand: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalSurfaceVariant)
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
            onFocusChanged = { }
        )
    }
}
