package org.github.ewt45.winemulator

import a.io.github.ewt45.winemulator.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.termux.x11.MainActivity
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.ui.theme.MainTheme
import org.github.ewt45.winemulator.viewmodel.MainViewModel
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel



class MainEmuActivity : MainActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val terminalViewModel: TerminalViewModel by viewModels()
    private val settingViewModel: SettingViewModel by viewModels()
    private lateinit var startX11ServiceIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        //设置包名
        MainActivity.HOST_PKG_NAME = packageName
        super.onCreate(savedInstanceState)
        //偏好设置
        prefs.showAdditionalKbd.put(false) // 不显示底部按键
        prefs.fullscreen.put(true) // 全屏
        prefs.hideCutout.put(false) // 挖孔屏等，先不在该区域显示吧。

        //将composeView添加到原视图布局中
        //TODO wrap不生效
        val composeView = ComposeView(this).apply {
            id = R.id.compose_view
            setContent {
                MainTheme {
                    MainScreen()
                }
            }
        }
        val frame = findViewById<FrameLayout>(com.termux.x11.R.id.frame)
        frame.addView(composeView, FrameLayout.LayoutParams(-2, -2))
        //缩小/放大
//        val foldBtn = Button(this).apply {
//            text = if (foldComposeView) "放大" else "缩小"
//            setOnClickListener { v ->
//                foldComposeView = !foldComposeView
//                text = if (foldComposeView) "放大" else "缩小"
//                composeView.layoutParams.height = if (foldComposeView) 100 else MATCH_PARENT
//                composeView.layoutParams.width = if (foldComposeView) 100 else MATCH_PARENT
//                composeView.requestLayout()
//            }
//        }
//        (frame.parent as ViewGroup).addView(foldBtn, FrameLayout.LayoutParams(-2, -2))

        enableEdgeToEdge()
//        setContent {
//            MainTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    MainScreen(modifier = Modifier.padding(innerPadding))
//                }
//            }
//        }

        lifecycleScope.launch {
            viewModel.showBlockDialog("解压alpine rootfs")
            try {
                Utils.Rootfs.ensureAlpineRootfs(this@MainEmuActivity)
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showBlockDialog("错误：解压alpine rootfs 失败！")
                return@launch
            }

            viewModel.closeBlockDialog()

            Utils.Rootfs.makeCurrent(Consts.alpineRootfsDir)
            terminalViewModel.startTerminal()
        }

        //启动xserver
        startX11ServiceIntent = Intent(this, X11Service::class.java)
        startService(startX11ServiceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(startX11ServiceIntent)
    }
}