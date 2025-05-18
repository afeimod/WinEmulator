package org.github.ewt45.winemulator.ui.setting

import android.content.Context
import android.system.OsConstants.SIGCONT
import android.system.OsConstants.SIGSTOP
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.github.ewt45.winemulator.Utils.getX11ServicePid
import org.github.ewt45.winemulator.ui.CollapsePanel
import org.github.ewt45.winemulator.ui.SimpleDialog
import org.github.ewt45.winemulator.viewmodel.PrepareStageViewModel
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel

@Composable
fun DebugSettings() {
    val terminalViewModel: TerminalViewModel = viewModel()
    val prepareStageViewModel: PrepareStageViewModel = viewModel()
    val ctx: Context = LocalContext.current
    DebugSettingsImpl(
        sendSigStop = {
            terminalViewModel.pauseTerminal()
            android.os.Process.sendSignal(ctx.getX11ServicePid(), SIGSTOP)
        },
        sendSigCont = {
            terminalViewModel.resumeTerminal()
            android.os.Process.sendSignal(ctx.getX11ServicePid(), SIGCONT)
        },
        gotoSelectRootfs = { prepareStageViewModel.setNoRootfs(true) }
    )
}

@Composable
fun DebugSettingsImpl(
    sendSigStop: () -> Unit = {},
    sendSigCont: () -> Unit = {},
    gotoSelectRootfs: () -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }
    SimpleDialog(showDialog, dialogText) { showDialog = it }
    val NOT_IMPL_YET = {
        dialogText = "尚未实现！"
        showDialog = true
    }
    CollapsePanel("调试选项") {
        Button(onClick = sendSigStop) { Text("向终端和x11发送STOP信号") }
        Button(onClick = sendSigCont) { Text("向终端和x11发送CONT信号") }
        Button(onClick = gotoSelectRootfs) { Text("进入选择rootfs界面") }
        Button(onClick = NOT_IMPL_YET) { Text("检查当前rootfs内文件是否有指向termux的软链接") }
    }

}