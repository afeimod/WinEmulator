package org.github.ewt45.winemulator

import a.io.github.ewt45.winemulator.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.termux.x11.MainActivity
import com.termux.x11.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.Consts.Pref.general_rootfs_lang
import org.github.ewt45.winemulator.Consts.Pref.proot_startup_cmd
import org.github.ewt45.winemulator.Utils.activityRecreate
import org.github.ewt45.winemulator.Utils.getX11ServicePid
import org.github.ewt45.winemulator.emu.X11Service
import org.github.ewt45.winemulator.emu.manager.EmuManager
import org.github.ewt45.winemulator.terminal.SessionClientAImpl
import org.github.ewt45.winemulator.terminal.ViewClientImpl
import org.github.ewt45.winemulator.ui.Destination
import org.github.ewt45.winemulator.ui.MainScreen
import org.github.ewt45.winemulator.ui.theme.MainTheme
import org.github.ewt45.winemulator.viewmodel.MainViewModel
import org.github.ewt45.winemulator.viewmodel.PrepareViewModel
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel


class MainEmuActivity : MainActivity() {
    private val TAG = "MainEmuActivity"
    val mainViewModel: MainViewModel by viewModels()
    val terminalViewModel: TerminalViewModel by viewModels()
    val settingViewModel: SettingViewModel by viewModels()
    val prepareViewModel: PrepareViewModel by viewModels()
    
    private lateinit var startX11Intent: Intent
    private var emuStarted: Boolean = false
    
    // 延迟初始化，确保 Activity 已完全准备好
    val sessionClient by lazy { SessionClientAImpl(this) }
    val viewClient by lazy { ViewClientImpl(this, sessionClient) }

    companion object {
        val instance get() = getInstance() as MainEmuActivity
    }

    fun getPref(): Prefs = prefs

    init {
        Utils.Permissions.registerForActivityResult(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.activityRecreate = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MainActivity.HOST_PKG_NAME = packageName
        startX11Intent = createStartX11Intent()
        super.onCreate(savedInstanceState)

        settingViewModel.initSharedPreferences(this)
        settingViewModel.syncX11SettingsToSharedPrefs()

        // 核心修复：直接从 viewModel 的 state 中获取设置，而不是调用不存在的方法
        lifecycleScope.launch {
            val x11Settings = settingViewModel.x11State.value
            prefs.displayResolutionMode.put("custom")
            prefs.displayResolutionCustom.put(x11Settings.resolution)
            prefs.fullscreen.put(x11Settings.fullscreen)
        }

        prefs.showAdditionalKbd.put(false)
        prefs.hideCutout.put(false)

        setContent {
            val themeMode by settingViewModel.themeState.collectAsState()
            val isDarkTheme = themeMode != 0

            MainTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    tx11Content = { frm.also { (frm.parent as? ViewGroup)?.removeView(frm) } },
                    Destination.X11, 
                    mainViewModel, 
                    terminalViewModel, 
                    settingViewModel, 
                    prepareViewModel
                )
            }
        }

        lifecycleScope.launch {
            prepareViewModel.uiState.collect { state ->
                if (state.isPrepareFinished && !emuStarted) {
                    startEmu()
                }
            }
        }

        enableEdgeToEdge()
    }

    suspend fun startEmu() = withContext(Dispatchers.Default) {
        if (emuStarted) return@withContext

        val selectedRootfs = Utils.Rootfs.getSelectedRootfs() ?: return@withContext
        Utils.Rootfs.makeCurrent(selectedRootfs)

        emuStarted = true

        val userName = settingViewModel.getCurrentLoginUser()
        terminalViewModel.updatePromptFromSettings(userName)

        if (Consts.rootfsCurrXkbDir.exists()) {
            startService(startX11Intent)
            waitForXStartedWithDialog()
        }

        // 启动原生终端
        terminalViewModel.startTerminal(sessionClient)
        
        withContext(Dispatchers.Main) {
            lifecycle.addObserver(EmuManager(lifecycleScope))
        }
        
        val lang = general_rootfs_lang.get()
        val langBase = lang.substringBefore('.')
        terminalViewModel.runCommand("""if ! locale -a | grep -qi "$langBase"; then locale-gen $lang; fi; export LANG=$lang""")
        
        proot_startup_cmd.get().takeIf { it.isNotBlank() }?.let {
            terminalViewModel.runCommand("$it &")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalViewModel.stopTerminal()
        stopService(startX11Intent)
        android.os.Process.killProcess(getX11ServicePid())
    }

    suspend fun waitForXStarted() {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 5000) {
            if (isConnected()) break
            else delay(200)
        }
    }

    suspend fun waitForXStartedWithDialog() {
        mainViewModel.showBlockDialog("xserver启动中") {
            waitForXStarted()
        }
    }

    override fun buildNotification(): Notification {
        val channelName = this.resources.getString(R.string.app_name)
        val channel = NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        return NotificationCompat.Builder(this, channelName)
            .setContentTitle(channelName)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("模拟器正在运行")
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createStartX11Intent(): Intent {
        return Intent(this, X11Service::class.java).apply {
            putExtra("timestamp", System.currentTimeMillis())
        }
    }
}
