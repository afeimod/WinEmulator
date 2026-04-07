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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    val TAG = "ProotOutputScreen"
    var execCommand by remember { mutableStateOf("") }
    
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
                //emmm加这个横向滚动 导致文本很短时，Text无法占满宽度了. 好了，在外层套一个Column就行了。
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            SelectionContainer {
                // 使用自定义的带颜色文本渲染
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
 * 带颜色的终端输出渲染
 * 识别提示符行并使用彩色样式渲染
 */
@Composable
fun ColoredTerminalOutput(
    output: List<String>,
    coloredPrompt: AnnotatedString
) {
    // 提示符的正则表达式模式：匹配 "用户名@主机:路径$" 或 "用户名@主机:路径#"
    // 例如：root@localhost:~$ 或 afei@localhost:~$
    val promptPattern = Regex("""^([\w]+)@([\w.]+):([/~][^\s]*)([#$])(\s*)$""")
    
    // 颜色定义
    val rootUserColor = Color(0xFFE0E0E0)  // root用户白色
    val normalUserColor = Color(0xFF64B5F6)  // 普通用户蓝色
    val hostColor = Color(0xFF4DD0E1)  // 主机名青色
    val pathColor = Color(0xFF81C784)  // 路径绿色
    val symbolColor = Color(0xFFFFD54F)  // 符号黄色
    
    // 构建带样式的输出
    val annotatedOutput = buildAnnotatedString {
        output.forEachIndexed { index, line ->
            // 检查这一行是否是提示符行
            val matchResult = promptPattern.find(line.trimEnd())
            
            if (matchResult != null) {
                // 这是提示符行，使用彩色样式
                val userName = matchResult.groupValues[1]
                val hostName = matchResult.groupValues[2]
                val path = matchResult.groupValues[3]
                val symbol = matchResult.groupValues[4]
                val trailing = matchResult.groupValues[5]
                
                // 用户名颜色：root为白色，其他为蓝色
                val userColor = if (userName == "root") rootUserColor else normalUserColor
                
                // 添加彩色提示符
                withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                    append(userName)
                }
                withStyle(SpanStyle(color = symbolColor)) {
                    append("@")
                }
                withStyle(SpanStyle(color = hostColor, fontWeight = FontWeight.Bold)) {
                    append(hostName)
                }
                withStyle(SpanStyle(color = symbolColor)) {
                    append(":")
                }
                withStyle(SpanStyle(color = pathColor, fontWeight = FontWeight.Bold)) {
                    append(path)
                }
                withStyle(SpanStyle(color = symbolColor)) {
                    append(symbol)
                    append(trailing)
                }
                
                // 如果原行有换行符，添加换行
                if (line.endsWith("\n")) {
                    append("\n")
                }
            } else {
                // 普通输出行，直接添加
                append(line)
            }
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
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * 兼容旧版本的函数（保留用于预览等场景）
 * 使用简化的预览输出，不依赖TerminalViewModel扩展
 */
@Composable
fun ProotTerminalScreenImpl(
    output: SnapshotStateList<String>,
    runCommand: (String) -> Unit,
    viewModel: TerminalViewModel? = null
) {
    // 创建简单的预览用彩色提示符
    val previewPrompt = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFE0E0E0), fontWeight = FontWeight.Bold)) {
                append("root")
            }
            withStyle(SpanStyle(color = Color(0xFFFFD54F))) {
                append("@")
            }
            withStyle(SpanStyle(color = Color(0xFF4DD0E1), fontWeight = FontWeight.Bold)) {
                append("localhost")
            }
            withStyle(SpanStyle(color = Color(0xFFFFD54F))) {
                append(":")
            }
            withStyle(SpanStyle(color = Color(0xFF81C784), fontWeight = FontWeight.Bold)) {
                append("~")
            }
            withStyle(SpanStyle(color = Color(0xFFFFD54F))) {
                append("# ")
            }
        }
    }
    
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
                ColoredTerminalOutput(
                    output = output.toList(),
                    coloredPrompt = previewPrompt
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
    val output = remember { mutableStateListOf<String>(
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "  终端开始运行",
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "",
        "root@localhost:~$ ls -la",
        "total 64",
        "drwxr-xr-x  5 root root 4096 Apr  7 08:00 .",
        "drwxr-xr-x  3 root root root 4096 Apr  7 08:00 ..",
        "-rw-r--r--  1 root root 4096 Apr  7 08:00 file1.txt",
        "root@localhost:~$ "
    ) }
    
    // 创建简单的预览用彩色提示符
    val previewPrompt = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFE0E0E0), fontWeight = FontWeight.Bold)) {
                append("root")
            }
            withStyle(SpanStyle(color = Color(0xFFFFD54F))) {
                append("@")
            }
            withStyle(SpanStyle(color = Color(0xFF4DD0E1), fontWeight = FontWeight.Bold)) {
                append("localhost")
            }
            withStyle(SpanStyle(color = Color(0xFFFFD54F))) {
                append(":")
            }
            withStyle(SpanStyle(color = Color(0xFF81C784), fontWeight = FontWeight.Bold)) {
                append("~")
            }
            withStyle(SpanStyle(color = Color(0xFFFFD54F))) {
                append("# ")
            }
        }
    }
    
    SimpleTerminalStatusBar(
        currentUser = "root",
        currentHost = "localhost",
        currentPath = "~",
        isConnected = true
    )
    
    Spacer(modifier = Modifier.height(4.dp))
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
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
                ColoredTerminalOutput(
                    output = output.toList(),
                    coloredPrompt = previewPrompt
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
