package org.github.ewt45.winemulator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RootOrange = Color(0xFFFF9800)
private val UserGreen = Color(0xFF4CAF50)
private val White = Color.White
private val Black = Color.Black

@Composable
fun ProotTerminalScreen(
    navController: Any? = null
) {
    val terminalLines = remember { mutableStateListOf<String>() }
    var currentInput by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf("user") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun runCommand(command: String) {
        if (command.isBlank()) return

        terminalLines.add("${currentUser}@localhost:$ $command")

        when (command.trim()) {
            "linbox" -> terminalLines.add("Launching Linbox...")
            "su" -> {
                currentUser = "root"
                terminalLines.add("Switched to root")
            }
            "exit" -> {
                currentUser = "user"
                terminalLines.add("Exited root")
            }
            "clear" -> terminalLines.clear()
            else -> terminalLines.add("Executed: $command")
        }

        currentInput = ""
    }

    LaunchedEffect(Unit) {
        if (terminalLines.isEmpty()) {
            runCommand("linbox")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(terminalLines) { _, line ->
                Text(
                    text = line,
                    color = White,
                    fontSize = 14.sp
                )
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
                            color = White,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    runCommand(currentInput)
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProotTerminalScreenPreview() {
    ProotTerminalScreen()
}