package org.github.ewt45.winemulator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

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
            .fillMaxHeight()
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }


        //有内容更新时自动滚动到最底部
        LaunchedEffect(output.size) {
            textVScroll.animateScrollTo(textVScroll.maxValue)
        }

        Spacer(modifier = Modifier.height(12.dp))

        val onSend = {
            viewModel.runCommand(execCommand)
            execCommand = ""
        }
        TextField(
            value = execCommand,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { execCommand = it },
            label = { Text("输入命令") },
            trailingIcon = {
                IconButton(onSend) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {onSend()})
        )
    }
}