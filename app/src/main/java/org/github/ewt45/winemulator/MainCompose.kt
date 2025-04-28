package org.github.ewt45.winemulator

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.github.ewt45.winemulator.ui.theme.MainTheme

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val TAG = "MainScreen"
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier) {
        ProotTerminalScreen()
    }

    // 阻塞对话框
    if (uiState.blockDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("加载中") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth() // 让 Column 填充对话框宽度
                        .wrapContentHeight(), // 根据内容调整高度
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(uiState.blockDialogMsg)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            },
            confirmButton = {}
        )
    }

}


@Composable
fun ProotTerminalScreen(
    modifier: Modifier = Modifier,
    viewModel: TerminalViewModel = viewModel(),
) {
    val TAG = "ProotOutputScreen"
    val output = viewModel.output

    val scope = rememberCoroutineScope()
    var execCommand by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
//            .widthIn(min=300.dp,max=700.dp)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Proot 终端",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        val textVScroll = rememberScrollState()
        Text(
            text = output.joinToString(separator = "\n"),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(textVScroll)
                //emmm加这个横向滚动 导致文本很短时，Text无法占满宽度了
//                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
        //有内容更新时自动滚动到最底部
        LaunchedEffect(output.size) {
            textVScroll.animateScrollTo(textVScroll.maxValue)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = execCommand,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { execCommand = it },
            label = { Text("输入命令") },
            trailingIcon = {
                IconButton(
                    onClick = {
                        viewModel.runCommand(execCommand)
                        execCommand = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送"
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    MainTheme {
        MainScreen()
        val terminalViewModel: TerminalViewModel = viewModel()
        val mainViewModel: MainViewModel = viewModel()
        LaunchedEffect(Unit) {
            terminalViewModel.output.add(
                "fsdfsdfsdfsdfsd" +
                        "fsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsdfsd11111" +
                        "1\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\\n\n\n\n\n\n\n\n\n\n\n\nsdfsdfsdfsdfsdfsd\n\n\n\n\n\n\n\n\n\n\n\n\\n\n\n\\n\n\n\n\n\n\\n\n\n\n\n\n\n\n\n\n\nfsdfsdfsdfsdfsdfsd"
            )

            mainViewModel.showBlockDialog("测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框测试对话框")
        }
    }
}

