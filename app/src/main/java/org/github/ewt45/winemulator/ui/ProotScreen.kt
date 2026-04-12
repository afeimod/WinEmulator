package org.github.ewt45.winemulator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

/**
 * 该界面已弃用。请统一使用 [TerminalScreen]。
 */
@Composable
fun ProotTerminalScreen(viewModel: TerminalViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("此界面已弃用，请使用原生终端。")
    }
}
