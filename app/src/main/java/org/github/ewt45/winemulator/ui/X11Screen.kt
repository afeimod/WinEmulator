package org.github.ewt45.winemulator.ui

import a.io.github.ewt45.winemulator.R
import android.content.Context
import android.util.Log
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
import com.termux.x11.input.InputEventSender
import com.termux.x11.input.InputStub
import com.termux.x11.input.RenderData
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.Utils.Ui.snapToNearestEdgeHalfway
import org.github.ewt45.winemulator.inputcontrols.InputControlsManager
import org.github.ewt45.winemulator.inputcontrols.InputControlsView
import org.github.ewt45.winemulator.inputcontrols.X11InputSender
import org.github.ewt45.winemulator.inputcontrols.InputEventHandler

/**
 * X11 Screen composable that displays X11 content with virtual controls overlay
 * 
 * This is the fixed version that properly routes input events from virtual controls
 * to the X11 session through the LorieView JNI bridge.
 */
@Composable
fun X11Screen(
    x11Content: (Context) -> View,
    onNavigateToOthers: (Destination) -> Unit,
    // Add callback to get LorieView from the X11 content view
    onLorieViewReady: ((InputStub) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // X11 Input Sender - 用于通过InputEventSender发送按键事件
    val x11InputSender = remember { X11InputSender() }
    
    // RenderData for touch events
    val renderData = remember { RenderData() }

    // 加载当前选中的虚拟按键配置 ID
    val currentProfileId = prefs.getInt(InputControlsFragment.SELECTED_PROFILE_ID, 0)
    val manager = remember { InputControlsManager(context) }
    val profile = remember(currentProfileId) {
        if (currentProfileId != 0) manager.getProfile(currentProfileId) else manager.getProfiles().firstOrNull()
    }

    // Create InputEventHandler that uses X11InputSender
    // InputEventHandler receives evdev keycodes from InputControlsView
    val inputEventHandler = remember {
        object : InputEventHandler {
            override fun onKeyEvent(keycode: Int, isDown: Boolean) {
                // keycode is evdev keycode from Binding class
                // X11InputSender will convert it to Android keycode and send via InputEventSender
                x11InputSender.sendEvdevKeyEvent(keycode, isDown)
            }

            override fun onPointerMove(dx: Int, dy: Int) {
                // Send mouse motion event (relative movement)
                x11InputSender.sendMouseMotionEvent(dx, dy)
            }

            override fun onPointerButton(button: Int, isDown: Boolean) {
                // Send mouse button event
                // button: 0=left, 1=right, 2=middle
                x11InputSender.sendMouseButtonEvent(button, isDown)
            }
        }
    }

    // Create InputControlsView with the event handler
    val inputControlsView = remember {
        InputControlsView(context, editMode = false).apply {
            profile?.let { setProfile(it) }
            this.inputEventHandler = inputEventHandler
            // 根据设置决定是否显示虚拟按键，默认关闭
            showTouchscreenControls = prefs.getBoolean("show_touchscreen_controls", false)
        }
    }

    // 监听显示设置的改变
    LaunchedEffect(prefs.getBoolean("show_touchscreen_controls", false)) {
        inputControlsView.showTouchscreenControls = prefs.getBoolean("show_touchscreen_controls", false)
    }

    // Listen for profile changes
    LaunchedEffect(currentProfileId) {
        val newProfile = if (currentProfileId != 0) manager.getProfile(currentProfileId) else manager.getProfiles().firstOrNull()
        inputControlsView.setProfile(newProfile)
    }

    // Box with X11 content and virtual controls overlay
    Box(Modifier.fillMaxSize()) {
        // X11 rendering content
        AndroidView(
            factory = { ctx ->
                val view = x11Content(ctx)
                
                // Try to get LorieView from the X11 content view
                // The x11Content should return a LorieView or a view that contains it
                try {
                    val lorieView = if (view is com.termux.x11.LorieView) {
                        view
                    } else {
                        // Try to find LorieView in the view hierarchy
                        findLorieView(view)
                    }
                    
                    lorieView?.let { 
                        // Initialize X11InputSender with the LorieView (which implements InputStub)
                        x11InputSender.initialize(it)
                        
                        // Setup render data for touch coordinate transformation
                        renderData.scale = android.graphics.PointF(1f, 1f) // Default scale
                        x11InputSender.renderData = renderData
                        
                        // Notify that LorieView is ready
                        onLorieViewReady?.invoke(it)
                        
                        Log.d("X11Screen", "X11InputSender initialized with LorieView")
                    } ?: run {
                        Log.e("X11Screen", "Could not find LorieView in X11 content")
                    }
                } catch (e: Exception) {
                    // If we can't get LorieView, log the error
                    Log.e("X11Screen", "Failed to initialize X11InputSender: ${e.message}", e)
                }
                
                view
            },
            modifier = Modifier.fillMaxSize()
        )

        // Virtual controls overlay
        AndroidView(
            factory = { inputControlsView },
            modifier = Modifier.fillMaxSize()
        )

        // Original minimize button
        MiniButton2(
            Modifier,
            onExpand = { onNavigateToOthers(Destination.ExceptX11) }
        )
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            x11InputSender.release()
        }
    }
}

/**
 * Recursively find LorieView in the view hierarchy
 */
private fun findLorieView(view: View): com.termux.x11.LorieView? {
    if (view is com.termux.x11.LorieView) {
        return view
    }
    
    if (view is android.view.ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            val result = findLorieView(child)
            if (result != null) {
                return result
            }
        }
    }
    
    return null
}

/** 
 * 用于在显示x11的视图时，点击展开其他视图 
 */
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
