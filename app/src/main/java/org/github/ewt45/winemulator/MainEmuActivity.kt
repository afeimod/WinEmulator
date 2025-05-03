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
import com.termux.x11.CmdEntryPoint
import com.termux.x11.LorieView
import com.termux.x11.MainActivity
import kotlinx.coroutines.delay
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

        startX11ServiceIntent = Intent(this, X11Service::class.java)
        startX11ServiceIntent.putExtra("timestamp", System.currentTimeMillis())

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

            //等待x11启动完成
            viewModel.showBlockDialog("xserver启动中") {
                waitForXStarted()
            }

            Utils.Rootfs.makeCurrent(Consts.alpineRootfsDir)
            terminalViewModel.startTerminal()
        }

        //启动xserver
        startService(startX11ServiceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(startX11ServiceIntent)
    }

    /**
     * 等待xserver启动完成
     */
    private suspend fun waitForXStarted() {
        while (true) {
            if (isConnected()) break
            else delay(200)
        }
    }
}