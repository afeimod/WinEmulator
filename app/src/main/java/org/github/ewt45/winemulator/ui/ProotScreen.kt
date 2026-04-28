package org.github.ewt45.winemulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.github.ewt45.winemulator.ui.theme.*
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

@Composable
fun ProotTerminalScreen(viewModel: TerminalViewModel) {
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }

    val scroll = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (viewModel.output.value.isEmpty()) {
            viewModel.runCommand("linbox")
        }
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(viewModel.output.value.size) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(scroll)
                .horizontalScroll(rememberScrollState())
                .clickable {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
        ) {
            viewModel.output.value.forEach { line ->
                Text(
                    text = line,
                    color = TerminalOnSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val userColor = if (viewModel.currentUser == "root") {
                    TerminalRootWhite
                } else {
                    TerminalUserGreen
                }

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = userColor,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(viewModel.currentUser)
                        }

                        withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                            append("@")
                        }

                        withStyle(SpanStyle(color = TerminalHostCyan)) {
                            append(viewModel.currentHost)
                        }

                        withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                            append(":")
                        }

                        withStyle(SpanStyle(color = TerminalPathWhite)) {
                            append(viewModel.currentPath)
                        }

                        withStyle(SpanStyle(color = TerminalSymbolYellow)) {
                            append("$ ")
                        }
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )

                BasicTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                    },
                    textStyle = TextStyle(
                        color = TerminalOnSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(TerminalCursor),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputValue.text.isNotBlank()) {
                                viewModel.runCommand(inputValue.text)
                                inputValue = TextFieldValue("")
                            }
                            keyboardController?.show()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .focusable(),
                    decorationBox = { innerTextField ->
                        innerTextField()
                    }
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ProotTerminalScreenPreview() {
}