package org.github.ewt45.winemulator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import a.io.github.ewt45.winemulator.R
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProotTerminalScreen(viewModel: TerminalViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var fontSize by remember { mutableStateOf(14.sp) }
    var showFontSlider by remember { mutableStateOf(false) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val historyCommands = remember { mutableStateListOf<String>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.output.value.size) {
        if (viewModel.output.value.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.output.value.lastIndex)
        }
    }

    fun executeCommand(cmd: String) {
        if (cmd.isNotBlank()) {
            historyCommands.add(cmd)
            historyIndex = -1
            viewModel.runCommand(cmd)
        }
        inputText = ""
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        ModernTerminalStatusBar(
            currentUser = viewModel.currentUser,
            currentHost = viewModel.currentHost,
            currentPath = viewModel.currentPath,
            isConnected = viewModel.isConnected,
            fontSize = fontSize,
            onFontSizeChange = { fontSize = it },
            showFontSlider = showFontSlider,
            onToggleFontSlider = { showFontSlider = !showFontSlider },
            onCopyAll = {
                val allText = viewModel.output.value.joinToString("\n")
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("terminal_output", allText))
                Toast.makeText(context, "已复制全部输出", Toast.LENGTH_SHORT).show()
            },
            onClearOutput = { viewModel.clearOutput() }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(viewModel.output.value) { index, line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = fontSize * 0.8f,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        ColoredTerminalOutputLine(
                            line = line,
                            fontSize = fontSize,
                            coloredPrompt = viewModel.getColoredPrompt()
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (historyCommands.isNotEmpty()) {
                            historyIndex = (historyIndex - 1).coerceAtLeast(0)
                            inputText = historyCommands.getOrElse(historyIndex) { "" }
                        }
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上一条命令")
                }
                IconButton(
                    onClick = {
                        if (historyCommands.isNotEmpty() && historyIndex < historyCommands.lastIndex) {
                            historyIndex++
                            inputText = historyCommands[historyIndex]
                        } else {
                            historyIndex = -1
                            inputText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下一条命令")
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入命令...", fontSize = fontSize) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(32.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { executeCommand(inputText) })
                )

                IconButton(onClick = { executeCommand(inputText) }) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTerminalStatusBar(
    currentUser: String,
    currentHost: String,
    currentPath: String,
    isConnected: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onFontSizeChange: (androidx.compose.ui.unit.TextUnit) -> Unit,
    showFontSlider: Boolean,
    onToggleFontSlider: () -> Unit,
    onCopyAll: () -> Unit,
    onClearOutput: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 使用项目已有的图标（如果不存在，可以暂时用 Text 代替）
                Icon(
                    painter = painterResource(
                        if (currentUser == "root") R.drawable.ic_terminal_root
                        else R.drawable.ic_terminal_user
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$currentUser@$currentHost:$currentPath",
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF9800))
                )
                TextButton(onClick = onCopyAll) { Text("复制", fontSize = 12.sp) }
                TextButton(onClick = onClearOutput) { Text("清屏", fontSize = 12.sp) }
                TextButton(onClick = onToggleFontSlider) { Text("字体", fontSize = 12.sp) }
            }
        }

        AnimatedVisibility(visible = showFontSlider) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                    .padding(12.dp)
            ) {
                Text("字体大小: ${fontSize.value.toInt()}sp", fontSize = 12.sp)
                Slider(
                    value = fontSize.value,
                    onValueChange = { onFontSizeChange(it.sp) },
                    valueRange = 10f..24f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ColoredTerminalOutputLine(
    line: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    coloredPrompt: AnnotatedString
) {
    val promptRegex = Regex("""([\w]+)@([\w.]+):([/~][^\s$#]*)([#$])(\s*)?$""")
    val match = promptRegex.find(line)

    val annotatedText = if (match != null && match.range.first == 0) {
        buildAnnotatedString {
            append(coloredPrompt)
            val afterPrompt = line.substring(match.range.last + 1)
            if (afterPrompt.isNotEmpty()) append(afterPrompt)
        }
    } else {
        AnnotatedString(line)
    }

    Text(
        text = annotatedText,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = fontSize * 1.3f
    )
}

@Preview(showBackground = true)
@Composable
fun ProotTerminalScreenPreview() {
    val mockViewModel = TerminalViewModel()
    ProotTerminalScreen(mockViewModel)
}