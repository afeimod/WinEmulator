package org.github.ewt45.winemulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

/**
 * Proot终端界面 - 集成美化后的终端UI
 * 支持显示用户名@主机:路径 格式的状态栏
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
            output = viewModel.output.value,
            onRunCommand = { viewModel.runCommand(it) }
        )
    }
}

/**
 * 终端内容区域 - 输出和输入
 */
@Composable
fun ProotTerminalContent(
    output: List<String>,
    onRunCommand: (String) -> Unit
) {
    val TAG = "ProotOutputScreen"
    var execCommand by remember { mutableStateOf("") }
    
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
                //emmm加这个横向滚动 导致文本很短时，Text无法占满宽度了. 好了，在外层套一个Column就行了。
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    text = output.joinToString(separator = ""),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }


        //有内容更新时自动滚动到最底部
        LaunchedEffect(output.size) {
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
 * 兼容旧版本的函数（保留用于预览等场景）
 */
@Composable
fun ProotTerminalScreenImpl(
    output: SnapshotStateList<String>,
    runCommand: (String) -> Unit,
) {
    ProotTerminalContent(
        output = output.toList(),
        onRunCommand = runCommand
    )
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
    
    SimpleTerminalStatusBar(
        currentUser = "root",
        currentHost = "localhost",
        currentPath = "~",
        isConnected = true
    )
    
    Spacer(modifier = Modifier.height(4.dp))
    
    ProotTerminalContent(
        output = output.toList(),
        onRunCommand = { output.add("root@localhost:~$ $it") }
    )
}
