package org.github.ewt45.winemulator.ui

import a.io.github.ewt45.winemulator.R
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.Utils.Ui.snapToNearestEdgeHalfway
import org.github.ewt45.winemulator.inputcontrols.InputEventHandler
import org.github.ewt45.winemulator.inputcontrols.InputControlsManager
import org.github.ewt45.winemulator.inputcontrols.InputControlsView

@Composable
fun X11Screen(x11Content: (Context) -> View, onNavigateToOthers: (Destination) -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // 加载当前选中的虚拟按键配置 ID
    val currentProfileId = prefs.getInt(InputControlsFragment.SELECTED_PROFILE_ID, 0)
    val manager = remember { InputControlsManager(context) }
    val profile = remember(currentProfileId) {
        if (currentProfileId != 0) manager.getProfile(currentProfileId) else manager.getProfiles().firstOrNull()
    }

    // 创建 InputControlsView，并设置配置和事件处理器
    val inputControlsView = remember {
        InputControlsView(context, editMode = false).apply {
            profile?.let { setProfile(it) }
            inputEventHandler = object : InputEventHandler {
                override fun onKeyEvent(keycode: Int, isDown: Boolean) {
                    // TODO: 替换为实际的 X11 输入调用
                    android.util.Log.d("X11Screen", "KeyEvent: $keycode, down=$isDown")
                }

                override fun onPointerMove(dx: Int, dy: Int) {
                    // TODO: 替换为实际的 X11 输入调用
                    android.util.Log.d("X11Screen", "PointerMove: $dx, $dy")
                }

                override fun onPointerButton(button: Int, isDown: Boolean) {
                    // TODO: 替换为实际的 X11 输入调用
                    android.util.Log.d("X11Screen", "MouseButton: $button, down=$isDown")
                }
            }
        }
    }

    // 监听配置变化，动态更新 InputControlsView
    LaunchedEffect(currentProfileId) {
        val newProfile = if (currentProfileId != 0) manager.getProfile(currentProfileId) else manager.getProfiles().firstOrNull()
        inputControlsView.setProfile(newProfile)
    }

    Box(Modifier.fillMaxSize()) {
        // X11 渲染内容
        AndroidView(
            factory = x11Content,
            modifier = Modifier.fillMaxSize()
        )

        // 虚拟按键覆盖层
        AndroidView(
            factory = { inputControlsView },
            modifier = Modifier.fillMaxSize()
        )

        // 原有的最小化按钮
        MiniButton2(
            Modifier,
            onExpand = { onNavigateToOthers(Destination.ExceptX11) }
        )
    }
}

/** 用于在显示x11的视图时，点击展开其他视图 */
@Composable
private fun MiniButton2(modifier: Modifier = Modifier, onExpand: () -> Unit) {
    val colorSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorContent = MaterialTheme.colorScheme.onSurface
    IconButton(
        onExpand,
        modifier,
        colors = IconButtonColors(colorSurface, colorContent, colorSurface, colorContent),
    ) {
        Icon(painterResource(R.drawable.ic_fullscreen), null)
    }
}

@Deprecated("现在不需要操作View了")
@Composable
private fun MiniButton(
    minimize: Boolean,
    onClick: () -> Unit,
) {
    val TAG = "MinimizeButton"
    val activity = LocalActivity.current
    val miniIconPx = (Consts.Ui.minimizedIconSize * LocalDensity.current.density).toInt()

    //最小化时颜色稍微变化一下吧，否则不容易看到
    val colorSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorContent = MaterialTheme.colorScheme.onSurface
    val colors =
        if (!minimize) IconButtonDefaults.iconButtonColors()
        else IconButtonColors(colorSurface, colorContent, colorSurface, colorContent)

    // 记住最小化时的位置。全屏后再次最小化时恢复到上一次位置而非默认位置
    val margin = remember { mutableListOf(0, 100) }

    IconButton(
        onClick = {
            val view = activity?.findViewById<View>(R.id.compose_view) ?: return@IconButton
            val nextValue = !minimize
            view.apply {
                val lp = layoutParams as MarginLayoutParams
                lp.height = if (nextValue) miniIconPx else MATCH_PARENT
                lp.width = if (nextValue) miniIconPx else MATCH_PARENT
                lp.leftMargin = if (nextValue) margin[0] else 0
                lp.topMargin = if (nextValue) margin[1] else 0
                lp.rightMargin = 0
                lp.bottomMargin = 0
                requestLayout()
                if (nextValue)
                    view.post { view.snapToNearestEdgeHalfway() }
            }
            onClick()
        },
        modifier = Modifier
            .size(Consts.Ui.minimizedIconSize.dp)
            .pointerInput(minimize) {
                if (!minimize)
                    return@pointerInput
                val view = activity?.findViewById<View>(R.id.compose_view) ?: return@pointerInput
                detectDragGestures(
                    onDragEnd = { view.snapToNearestEdgeHalfway() }
                ) { change, dragAmount ->
                    change.consume()
                    val lp = view.layoutParams as MarginLayoutParams
                    lp.leftMargin += dragAmount.x.toInt()
                    lp.topMargin += dragAmount.y.toInt()
                    margin[0] = lp.leftMargin
                    margin[1] = lp.topMargin
                    view.requestLayout()
                }
            },
        colors = colors
    ) {
        Icon(
            painter = painterResource(if (minimize) R.drawable.ic_fullscreen else R.drawable.ic_hide),
            contentDescription = "全屏/最小化",
        )
    }
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
fun X11ScreenPreview() {
    X11Screen(
        x11Content = { ctx -> FrameLayout(ctx).apply { setBackgroundColor(android.graphics.Color.GRAY) } },
        {}
    )
}