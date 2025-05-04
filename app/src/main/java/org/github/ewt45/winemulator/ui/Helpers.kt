package org.github.ewt45.winemulator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 从中心放大缩小
 */
@Composable
fun AnimatedSizeInCenter(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        //animationSpec = tween(durationMillis = 300),
        enter = expandIn(expandFrom = Alignment.Center) + fadeIn(),
        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
    ) {
        content()
    }
}

/**
 * 显示一个对话框。
 * @param onDismiss 对话框关闭时的回调，应该在此时将visible改为false
 * @param onConfirm 点击确定按钮时的回调
 * @param onCancel  点击取消按钮时的回调
 * @param hideBtns 是否隐藏按钮以防止关闭, 为true时传入的onDismiss不会被执行
 */
@Composable
fun Dialog(
    text: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = {},
    onCancel: (() -> Unit)? = null,
    hideBtns: Boolean = false,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = { if (!hideBtns) onDismiss() },
//                title = { Text("加载中") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth() // 让 Column 填充对话框宽度
                        .wrapContentHeight(), // 根据内容调整高度
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            },
            confirmButton = { if (!hideBtns) TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) } },
            dismissButton = { if (!hideBtns && onCancel != null) TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

}

@Preview
@Composable
private fun Test() {
    Dialog(
        visible = true,
        text = "111",
        onDismiss = {}
    )
}