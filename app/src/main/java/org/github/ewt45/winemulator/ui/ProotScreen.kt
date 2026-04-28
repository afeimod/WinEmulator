package org.github.ewt45.winemulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview

private val RootOrange = Color(0xFFFF9800)
private val UserGreen = Color(0xFF4CAF50)
private val TerminalText = Color.White

@Composable
fun ProotTerminalScreen() {
    val terminalLines = remember { mutableStateListOf<String>() }
    var currentInput by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf("user") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun executeCommand(command: String) {
        if (command.isBlank()) return

        terminalLines.add("${currentUser}@localhost:$ $command")

        when (command.trim()) {
            "linbox" -> {
                terminalLines.add("Starting Linbox environment...")
            }

            "su" -> {
                currentUser = "root"
                terminalLines.add("Switched to root")
            }

            "exit" -> {
                if (currentUser == "root") {
                    currentUser = "user"
                    terminalLines.add("Exited root shell")
                }
            }

            "clear" -> {
                terminalLines.clear()
            }

            else -> {
                terminalLines.add("Command executed: $command")
            }
        }

        currentInput = ""
    }

    LaunchedEffect(Unit) {
        if (terminalLines.isEmpty()) {
            executeCommand("linbox")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = false
        ) {
            items(terminalLines) { line ->
                SelectionContainer {
                    Text(
                        text = line,
                        color = TerminalText,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentUser}@localhost:$ ",
                        color = if (currentUser == "root") RootOrange else UserGreen,
                        fontSize = 14.sp
                    )

                    BasicTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        textStyle = TextStyle(
                            color = TerminalText,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "↵ Enter to run",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.clickable {
                executeCommand(currentInput)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProotTerminalScreenPreview() {
    ProotTerminalScreen()
}