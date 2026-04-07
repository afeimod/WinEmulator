package org.github.ewt45.winemulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

/**
 * Proot终端界面 - 集成美化后的终端UI
 * 支持显示用户名@主机:路径 格式的状态栏
 * 支持彩色提示符和键盘适配
 */
@Composable
fun ProotTerminalScreen(viewModel: TerminalViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部状态栏 - 显示用户名、主机名和当前目录
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
 * 终端内容区域 - 输出和输入
 * 支持键盘弹出时自动调整布局
 */
@Composable
fun ProotTerminalContent(
    viewModel: TerminalViewModel,
    onRunCommand: (String) -> Unit
) {
    var execCommand by remember { mutableStateOf("") }
    
    // 将原始输出合并为完整文本（每个列表元素已包含换行符）
    val fullOutputText by remember {
        derivedStateOf {
            viewModel.output.value.joinToString("")
        }
    }
    
    // 解析 ANSI 转义序列，生成带颜色的 AnnotatedString
    val coloredOutput by remember(fullOutputText) {
        derivedStateOf {
            parseAnsiToAnnotatedString(fullOutputText)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .imePadding() // 自动处理键盘弹出时的padding
    ) {
        val textVScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(textVScroll)
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            SelectionContainer {
                // 使用带颜色的终端输出
                Text(
                    text = coloredOutput,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 有内容更新时自动滚动到最底部
        LaunchedEffect(viewModel.output.value) {
            textVScroll.animateScrollTo(textVScroll.maxValue)
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // 命令输入区域美化
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(4.dp)
        ) {
            TextField(
                value = execCommand,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { execCommand = it },
                label = { 
                    Text(
                        text = "输入命令",
                        fontFamily = FontFamily.Monospace
                    ) 
                },
                placeholder = {
                    Text(
                        text = "请输入命令...",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (execCommand.isNotBlank()) {
                                onRunCommand(execCommand)
                                execCommand = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (execCommand.isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (execCommand.isNotBlank()) {
                            onRunCommand(execCommand)
                            execCommand = ""
                        }
                    }
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 解析 ANSI 转义序列（SGR 码）并转换为带样式的 AnnotatedString
 * 支持：
 * - 前景色：30-37（标准色），90-97（亮色）
 * - 背景色：40-47，100-107
 * - 样式：1（粗体），3（斜体），4（下划线），0（重置）
 * - 多个代码组合，如 "1;31" 表示粗体红色
 */
private fun parseAnsiToAnnotatedString(text: String): AnnotatedString {
    val result = AnnotatedString.Builder()
    var currentStyle = SpanStyle()
    val buffer = StringBuilder()
    
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\u001B' && i + 1 < text.length && text[i + 1] == '[') {
            // 遇到转义序列，先输出缓冲区的文本
            if (buffer.isNotEmpty()) {
                result.pushStyle(currentStyle)
                result.append(buffer.toString())
                result.pop()
                buffer.clear()
            }
            
            // 解析 ESC[ ... m
            var j = i + 2
            while (j < text.length && text[j] != 'm') {
                j++
            }
            if (j < text.length) {
                val codeStr = text.substring(i + 2, j)
                val codes = codeStr.split(';').mapNotNull { it.toIntOrNull() }
                currentStyle = applySgrCodes(codes, currentStyle)
                i = j + 1
                continue
            } else {
                // 未找到结束符，按普通字符处理
                buffer.append(c)
                i++
                continue
            }
        } else {
            buffer.append(c)
            i++
        }
    }
    
    // 输出剩余文本
    if (buffer.isNotEmpty()) {
        result.pushStyle(currentStyle)
        result.append(buffer.toString())
        result.pop()
    }
    
    return result.toAnnotatedString()
}

/**
 * 根据 ANSI SGR 代码更新 SpanStyle
 */
private fun applySgrCodes(codes: List<Int>, currentStyle: SpanStyle): SpanStyle {
    var newStyle = currentStyle
    for (code in codes) {
        when (code) {
            0 -> newStyle = SpanStyle() // 重置所有样式
            1 -> newStyle = newStyle.copy(fontWeight = FontWeight.Bold)
            3 -> newStyle = newStyle.copy(fontStyle = FontStyle.Italic)
            4 -> newStyle = newStyle.copy(textDecoration = TextDecoration.Underline)
            in 30..37 -> newStyle = newStyle.copy(color = ansiForegroundColor(code))
            in 40..47 -> newStyle = newStyle.copy(background = ansiBackgroundColor(code))
            in 90..97 -> newStyle = newStyle.copy(color = ansiBrightForegroundColor(code))
            in 100..107 -> newStyle = newStyle.copy(background = ansiBrightBackgroundColor(code))
        }
    }
    return newStyle
}

// 标准前景色映射（30-37）
private fun ansiForegroundColor(code: Int): Color = when (code) {
    30 -> Color.Black
    31 -> Color(0xFFD32F2F) // 红
    32 -> Color(0xFF388E3C) // 绿
    33 -> Color(0xFFF57C00) // 黄/橙
    34 -> Color(0xFF1976D2) // 蓝
    35 -> Color(0xFF7B1FA2) // 紫
    36 -> Color(0xFF0097A7) // 青
    37 -> Color(0xFFF5F5F5) // 白
    else -> Color.White
}

// 标准背景色映射（40-47）
private fun ansiBackgroundColor(code: Int): Color = when (code) {
    40 -> Color.Black
    41 -> Color(0xFFD32F2F)
    42 -> Color(0xFF388E3C)
    43 -> Color(0xFFF57C00)
    44 -> Color(0xFF1976D2)
    45 -> Color(0xFF7B1FA2)
    46 -> Color(0xFF0097A7)
    47 -> Color(0xFFF5F5F5)
    else -> Color.Transparent
}

// 亮前景色映射（90-97）
private fun ansiBrightForegroundColor(code: Int): Color = when (code) {
    90 -> Color(0xFF9E9E9E)
    91 -> Color(0xFFEF5350)
    92 -> Color(0xFF66BB6A)
    93 -> Color(0xFFFFA726)
    94 -> Color(0xFF42A5F5)
    95 -> Color(0xFFAB47BC)
    96 -> Color(0xFF26C6DA)
    97 -> Color.White
    else -> Color.White
}

// 亮背景色映射（100-107）
private fun ansiBrightBackgroundColor(code: Int): Color = when (code) {
    100 -> Color(0xFF616161)
    101 -> Color(0xFFEF5350)
    102 -> Color(0xFF66BB6A)
    103 -> Color(0xFFFFA726)
    104 -> Color(0xFF42A5F5)
    105 -> Color(0xFFAB47BC)
    106 -> Color(0xFF26C6DA)
    107 -> Color.White
    else -> Color.Transparent
}

/**
 * 兼容旧版本的函数（保留用于预览等场景）
 * 使用简化的预览输出，不依赖TerminalViewModel扩展
 */
@Composable
fun ProotTerminalScreenImpl(
    output: List<String>,
    runCommand: (String) -> Unit,
    viewModel: TerminalViewModel? = null
) {
    val fullText = remember(output) { output.joinToString("") }
    val coloredOutput = remember(fullText) { parseAnsiToAnnotatedString(fullText) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .imePadding()
    ) {
        val textVScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(textVScroll)
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    text = coloredOutput,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(4.dp)
        ) {
            TextField(
                value = "",
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { },
                label = { 
                    Text(
                        text = "输入命令",
                        fontFamily = FontFamily.Monospace
                    ) 
                },
                placeholder = {
                    Text(
                        text = "请输入命令...",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                enabled = false,
                trailingIcon = { }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Proot终端预览函数
 */
@Composable
fun ProotTerminalScreenPreview() {
    val output = listOf(
        "\u001B[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001B[0m\n",
        "\u001B[32m  终端开始运行\u001B[0m\n",
        "\u001B[36m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\u001B[0m\n",
        "\n",
        "\u001B[1;32mroot\u001B[0m\u001B[1;33m@\u001B[0m\u001B[1;36mlocalhost\u001B[0m\u001B[1;33m:\u001B[0m\u001B[1;34m~\u001B[0m\u001B[1;33m$ \u001B[0mls --color=always\n",
        "\u001B[0m\u001B[01;34mtotal 64\u001B[0m\n",
        "\u001B[01;34mdrwxr-xr-x\u001B[0m \u001B[01;34m5\u001B[0m \u001B[34;01mroot\u001B[0m \u001B[34;01mroot\u001B[0m \u001B[01;34m4096\u001B[0m \u001B[35;01mApr\u001B[0m \u001B[00m7\u001B[0m \u001B[35;01m08:00\u001B[0m \u001B[01;34m.\u001B[0m\n",
        "\u001B[01;34mdrwxr-xr-x\u001B[0m \u001B[01;34m3\u001B[0m \u001B[34;01mroot\u001B[0m \u001B[34;01mroot\u001B[0m \u001B[01;34m4096\u001B[0m \u001B[35;01mApr\u001B[0m \u001B[00m7\u001B[0m \u001B[35;01m08:00\u001B[0m \u001B[01;34m..\u001B[0m\n",
        "\u001B[0m\u001B[01;34m-rw-r--r--\u001B[0m \u001B[01;34m1\u001B[0m \u001B[34;01mroot\u001B[0m \u001B[34;01mroot\u001B[0m \u001B[01;34m4096\u001B[0m \u001B[35;01mApr\u001B[0m \u001B[00m7\u001B[0m \u001B[35;01m08:00\u001B[0m \u001B[00mfile1.txt\u001B[0m\n",
        "\u001B[1;32mroot\u001B[0m\u001B[1;33m@\u001B[0m\u001B[1;36mlocalhost\u001B[0m\u001B[1;33m:\u001B[0m\u001B[1;34m~\u001B[0m\u001B[1;33m$ \u001B[0m"
    )
    
    SimpleTerminalStatusBar(
        currentUser = "root",
        currentHost = "localhost",
        currentPath = "~",
        isConnected = true
    )
    
    Spacer(modifier = Modifier.height(4.dp))
    
    ProotTerminalScreenImpl(output, {}, null)
}