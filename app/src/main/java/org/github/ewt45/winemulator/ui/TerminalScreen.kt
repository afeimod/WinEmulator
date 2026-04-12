package org.github.ewt45.winemulator.ui

import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import org.github.ewt45.winemulator.terminal.ViewClientImpl
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    viewClient: ViewClientImpl? = null
) {
    TerminalScreenImpl(
        viewClient = viewClient,
        terminalSession = viewModel.terminalSession,
        currentUser = viewModel.currentUser,
        currentHost = viewModel.currentHost,
        currentPath = viewModel.currentPath,
        isConnected = viewModel.isConnected
    )
}

@Composable
private fun TerminalScreenImpl(
    viewClient: ViewClientImpl?,
    terminalSession: TerminalSession?,
    currentUser: String,
    currentHost: String,
    currentPath: String,
    isConnected: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        TerminalStatusBar(currentUser, currentHost, currentPath, isConnected)
        
        HorizontalDivider(
            thickness = 0.5.dp, 
            color = Color.White.copy(alpha = 0.1f)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        isClickable = true
                        
                        viewClient?.let { setTerminalViewClient(it) }
                        setTextSize(14)
                        
                        terminalSession?.let { attachSession(it) }
                        
                        setOnTouchListener { v, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                v.requestFocus()
                                v.requestFocusFromTouch()
                                v.post {
                                    val imm = ctx.getSystemService(InputMethodManager::class.java)
                                    imm?.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                                }
                            }
                            false 
                        }
                    }
                },
                update = { view ->
                    if (terminalSession != null && view.mTermSession != terminalSession) {
                        view.attachSession(terminalSession)
                    }
                    view.post {
                        if (view.width > 0 && view.height > 0) {
                            view.updateSize()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun TerminalStatusBar(
    currentUser: String,
    currentHost: String,
    currentPath: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF00FF00) else Color(0xFFFFCC00))
        )
        Spacer(Modifier.width(8.dp))
        
        Text(
            text = "$currentUser@$currentHost",
            fontSize = 12.sp,
            color = if (currentUser == "root") Color(0xFFFF5555) else Color(0xFF55FFFF),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = ":",
            fontSize = 12.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
        
        Text(
            text = currentPath,
            fontSize = 12.sp,
            color = Color(0xFFFFFF55),
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = if (isConnected) "ONLINE" else "OFFLINE",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * 补全预览函数，修复编译错误
 */
@Composable
fun TerminalScreenPreview() {
    TerminalScreenImpl(
        viewClient = null,
        terminalSession = null,
        currentUser = "root",
        currentHost = "localhost",
        currentPath = "~",
        isConnected = true
    )
}
