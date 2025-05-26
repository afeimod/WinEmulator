package org.github.ewt45.winemulator

import a.io.github.ewt45.winemulator.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.termux.x11.MainActivity
import com.termux.x11.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.github.ewt45.winemulator.Consts.Pref.general_rootfs_lang
import org.github.ewt45.winemulator.Consts.Pref.proot_startup_cmd
import org.github.ewt45.winemulator.Utils.activityRecreate
import org.github.ewt45.winemulator.Utils.getX11ServicePid
import org.github.ewt45.winemulator.emu.X11Service
import org.github.ewt45.winemulator.emu.manager.EmuManager
import org.github.ewt45.winemulator.terminal.SessionClientAImpl
import org.github.ewt45.winemulator.terminal.ViewClientImpl
import org.github.ewt45.winemulator.ui.MainScreen
import org.github.ewt45.winemulator.ui.theme.MainTheme
import org.github.ewt45.winemulator.viewmodel.MainViewModel
import org.github.ewt45.winemulator.viewmodel.PrepareStageViewModel
import org.github.ewt45.winemulator.viewmodel.SettingViewModel
import org.github.ewt45.winemulator.viewmodel.TerminalViewModel


class MainEmuActivity : MainActivity() {
    private val TAG = "MainEmuActivity"
    val viewModel: MainViewModel by viewModels()
    val terminalViewModel: TerminalViewModel by viewModels()
    val settingViewModel: SettingViewModel by viewModels()
    val prepareViewModel: PrepareStageViewModel by viewModels()
    private lateinit var startX11Intent: Intent
    private var emuStarted: Boolean = false
    val sessionClient: SessionClientAImpl = SessionClientAImpl(this)
    val viewClient: ViewClientImpl = ViewClientImpl(this, sessionClient)

    companion object {
        val instance get() = getInstance() as MainEmuActivity // val instance: MainEmuActivity by lazy { getInstance() as MainEmuActivity }
    }

    fun getPref(): Prefs = prefs

    init {
        Utils.Permissions.registerForActivityResult(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.activityRecreate = true
        Log.d(TAG, "进入onSaveInstanceState1, 保存数据")

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState?.activityRecreate == true) {
            Log.e(TAG, "进入onCreate 本次为重启。或许应该特殊处理。")
        }

        //设置包名
        MainActivity.HOST_PKG_NAME = packageName
        startX11Intent = createStartX11Intent()
        super.onCreate(savedInstanceState)

        //偏好设置
        prefs.displayResolutionMode.put("custom")
        runBlocking { prefs.displayResolutionCustom.put(settingViewModel.generalFLow.first().resolution) }
        prefs.showAdditionalKbd.put(false) // 不显示底部按键
//        prefs.fullscreen.put(true) // 全屏 // FIXME 这个变更会导致重建activity. 所以如果修改的话先不做其他操作了
        prefs.hideCutout.put(false) // 挖孔屏等，先不在该区域显示吧。


//        //将composeView添加到原视图布局中
//        val composeView = ComposeView(this).apply {
//            id = R.id.compose_view
//            setContent {
//                MainTheme {
//                    MainScreen()
//                }
//            }
//        }
//        val frame = findViewById<FrameLayout>(com.termux.x11.R.id.frame)
//        frame.addView(composeView, FrameLayout.LayoutParams(-2, -2))

        // 将原视图放到compose中
        (frm.parent as? ViewGroup)?.removeView(frm)
        setContent {
            Box {
                MainScreen(tx11Content = { frm })
            }
        }

        enableEdgeToEdge()

        val noRootfs = Utils.Rootfs.haveNoRootfs()
        val haveStoragePermission = Utils.Permissions.checkStoragePermission(this)
        prepareViewModel.setNoRootfs(noRootfs)
        if (!noRootfs && haveStoragePermission) {
            startEmu()

            //尝试termux终端
        }
    }

    fun startEmu() {
        if (emuStarted) {
            Log.w(TAG, "prepareAndStart: emuStarted为true, 模拟器已经启动。不再执行逻辑")
            return
        }
        // TODO 这里launch切换到IO协程会不会好一点？
        lifecycleScope.launch {
//            Log.d(TAG, "prepareAndStart: 测试process输出？${Utils.readLinesProcessOutput(Runtime.getRuntime().exec(arrayOf("sh",
//                "-c",
//                "umask 0022 ; ls /storage/emulated/0",//sh -c 之后应该用一个字符串 不应再分割了
//                )))}")

            val selectedRootfs = Utils.Rootfs.getSelectedRootfs()
            if (selectedRootfs == null) {
                prepareViewModel.setNoRootfs(true)
                Log.e(TAG, "prepareAndStart: 未找到可用的rootfs，请在执行此函数前提醒用户选择rootfs")
                return@launch
            }
            //rootfs处理（目前绑定外部存储路径在Proot里执行）
            Utils.Rootfs.makeCurrent(selectedRootfs)

            emuStarted = true

            //启动xserver
            if (Consts.rootfsCurrXkbDir.exists()) {
                // debug没事release就有事？？
                if (!isConnected()) {
                    startService(startX11Intent)
                    waitForXStartedWithDialog() // 等待x11启动完成
                }
            } else {
                viewModel.showConfirmDialog("rootfs下缺少xkb文件夹，x11不会启动。可以安装类似 ' libxkbcommon-x11 ' 的软件包来补全。")
            }

            //这里还不能用state因为state第一次获取的是默认值而非datastore来的值
            terminalViewModel.startTerminal()
            //添加observer时会立刻发送一遍从头到现在的状态，所以onCreate会触发
            lifecycle.addObserver(EmuManager(lifecycleScope))
            val LANG = general_rootfs_lang.get()
            // grep的$LANG应该还是从环境变量获取 因为有时候如果没生效的话LANG会被还原会C,可以用这个判断是否需要
            terminalViewModel.runCommand("""if [ "$(locale -a | grep ${'$'}LANG)" != $LANG ]; then locale-gen; fi; export LANG=$LANG""")
            proot_startup_cmd.get().takeIf { it.isNotBlank() }?.let {
                terminalViewModel.runCommand("$it &")
            }


        }
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalViewModel.stopTerminal()
        stopService(startX11Intent)
        // FIXME 目前release构建 finish 无法结束 service 进程 导致下次启动 xserver启动失败。需要手动强制结束进程
        android.os.Process.killProcess(getX11ServicePid())

        // 删除通知 从onPause改到onDestroy
        val notificationManager = getSystemService(NotificationManager::class.java)
        val mNotificationId = 7892
        for (notification in notificationManager.activeNotifications)
            if (notification.id == mNotificationId)
                notificationManager.cancel(mNotificationId)
    }

    /**
     * 等待xserver启动完成。最多等待5秒
     */
    suspend fun waitForXStarted() {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 5000) {
            if (isConnected()) break
            else delay(200)
        }
    }

    suspend fun waitForXStartedWithDialog() {
        viewModel.showBlockDialog("xserver启动中") {
            waitForXStarted()
        }
    }

    override fun buildNotification(): Notification {
        val channelName = this.resources.getString(R.string.app_name)
        val channel = NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) channel.setAllowBubbles(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val builder: NotificationCompat.Builder =
            (NotificationCompat.Builder(this, channelName)).setContentTitle(channelName)
                .setSmallIcon(R.mipmap.ic_launcher).setContentText("模拟器正在运行")
                .setOngoing(true).setPriority(NotificationCompat.PRIORITY_MAX)
                .setSilent(true).setShowWhen(false)
//                .setContentIntent(PendingIntent.getActivity(this, 0, Intent.makeMainActivity(componentName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        //.setColor(-10453621)
        return builder.build()
    }

    /**
     * 创建一个intent用于启动X11Service. 在intent放入数据：
     * timestamp：时间戳
     *
     */
    private fun createStartX11Intent(): Intent {
        return Intent(this, X11Service::class.java).apply {
            putExtra("timestamp", System.currentTimeMillis())
        }
    }
}