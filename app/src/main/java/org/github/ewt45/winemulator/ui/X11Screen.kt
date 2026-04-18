package org.github.ewt45.winemulator.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.termux.x11.input.InputStub
import com.termux.x11.input.RenderData
import org.github.ewt45.winemulator.inputcontrols.InputControlsManager
import org.github.ewt45.winemulator.inputcontrols.InputControlsView
import org.github.ewt45.winemulator.inputcontrols.X11InputSender
import org.github.ewt45.winemulator.inputcontrols.InputEventHandler
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import kotlinx.coroutines.delay

/**
 * X11 Screen composable that displays X11 content with virtual controls overlay
 *
 * This screen includes:
 * - X11 rendering content from LorieView
 * - Virtual controls overlay
 * - Expandable floating menu with independent popup windows for:
 *   - General settings (container language, shared folders, rootfs management)
 *   - Virtual keys settings (enable/disable, profile selection, layout editing)
 *   - X11 display settings (resolution, touch mode, orientation, scale)
 *
 * Input events from virtual controls are routed through the X11InputSender
 * to the X11 session via the LorieView JNI bridge.
 */
@Composable
fun X11Screen(
    x11Content: (Context) -> View,
    onNavigateToOthers: (Destination) -> Unit,
    // Add callback to get LorieView from the X11 content view
    onLorieViewReady: ((InputStub) -> Unit)? = null,
    // 新增：用于悬浮弹窗的SettingViewModel
    settingVm: SettingViewModel? = null
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    // X11 Input Sender - 用于通过InputEventSender发送按键事件
    val x11InputSender = remember { X11InputSender() }
    // RenderData for touch events
    val renderData = remember { RenderData() }
    // 加载当前选中的虚拟按键配置 ID - 改为响应式状态变量
    var currentProfileId by remember { mutableStateOf(prefs.getInt(InputControlsFragment.SELECTED_PROFILE_ID, 0)) }
    val manager = remember { InputControlsManager(context) }
    val profile = remember(currentProfileId) {
        if (currentProfileId != 0) manager.getProfile(currentProfileId) else manager.getProfiles().firstOrNull()
    }

    // 用于监听show_touchscreen_controls的变化
    var showTouchscreenControls by remember { mutableStateOf(prefs.getBoolean("show_touchscreen_controls", false)) }

    // 创建一个复合key用于强制重组InputControlsView
    var viewKey by remember { mutableStateOf(0L) }

    // 使用轮询方式监听SharedPreferences变化（比监听器更可靠）
    LaunchedEffect(Unit) {
        while (true) {
            val newShowControls = prefs.getBoolean("show_touchscreen_controls", false)
            val newProfileId = prefs.getInt(InputControlsFragment.SELECTED_PROFILE_ID, 0)
            
            // 只有值真正变化时才更新
            if (newShowControls != showTouchscreenControls || newProfileId != currentProfileId) {
                Log.d("X11Screen", "Settings changed: showControls=$newShowControls, profileId=$newProfileId")
                showTouchscreenControls = newShowControls
                currentProfileId = newProfileId
                viewKey++ // 递增key强制重组
            }
            
            delay(300) // 每300ms检查一次
        }
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
    // 使用viewKey作为key确保状态变化时强制重组
    val inputControlsView = remember(viewKey) {
        Log.d("X11Screen", "Creating InputControlsView with showControls=$showTouchscreenControls, profileId=$currentProfileId")
        InputControlsView(context, editMode = false).apply {
            val profileToSet = if (currentProfileId != 0) manager.getProfile(currentProfileId) else manager.getProfiles().firstOrNull()
            profileToSet?.let { setProfile(it) }
            this.inputEventHandler = inputEventHandler
            // 根据设置决定是否显示虚拟按键
            showTouchscreenControls = showTouchscreenControls
        }
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
        // 悬浮弹窗状态管理
        val floatingPopupState = remember { FloatingPopupState() }

        // 悬浮球：使用 BoxWithConstraints 获取父布局尺寸
        BoxWithConstraints(
            Modifier.fillMaxSize()
        ) {
            // 使用新的可展开悬浮菜单
            ExpandableFloatingMenu(
                parentWidth = constraints.maxWidth.toFloat(),
                parentHeight = constraints.maxHeight.toFloat(),
                onMainMenuClick = { onNavigateToOthers(Destination.Terminal) },
                onGeneralSettingsClick = { floatingPopupState.showPopup(FloatingPopupType.GENERAL_SETTINGS) },
                onVirtualKeysClick = { floatingPopupState.showPopup(FloatingPopupType.VIRTUAL_KEYS_SETTINGS) },
                onX11SettingsClick = { floatingPopupState.showPopup(FloatingPopupType.X11_SETTINGS) }
            )
        }

        // 显示悬浮弹窗
        settingVm?.let { vm ->
            FloatingSettingsPopups(
                popupState = floatingPopupState,
                settingVm = vm
            )
        }
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

@Preview(widthDp = 300, heightDp = 500)
@Composable
fun X11ScreenPreview() {
    X11Screen(
        x11Content = { ctx -> FrameLayout(ctx).apply { setBackgroundColor(android.graphics.Color.GRAY) } },
        {},
        settingVm = null
    )
}
