package org.github.ewt45.winemulator.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.Utils.Ui.editDateStore
import org.github.ewt45.winemulator.Utils.Ui.stateInSimple
import org.github.ewt45.winemulator.dataStore
import org.github.ewt45.winemulator.Consts.Pref
import org.github.ewt45.winemulator.Consts.Pref.Local.rootfs_login_user_json
import org.github.ewt45.winemulator.Consts.Pref.general_resolution
import org.github.ewt45.winemulator.Consts.Pref.general_rootfs_lang
import org.github.ewt45.winemulator.Consts.Pref.general_shared_ext_path
import org.github.ewt45.winemulator.Consts.Pref.proot_bool_options
import org.github.ewt45.winemulator.Consts.Pref.proot_startup_cmd
import org.github.ewt45.winemulator.FuncOnChangeAction
import org.github.ewt45.winemulator.MainEmuActivity
import org.github.ewt45.winemulator.RateLimiter
import org.github.ewt45.winemulator.Utils
import org.github.ewt45.winemulator.Utils.Ui.editDateStoreAsync
import org.github.ewt45.winemulator.emu.ProotRootfs
import java.io.File

private val TAG = "SettingViewModel"


/**
 * 修改数据逻辑： 用户编辑 -> 调用viewModel函数 -> 修改dataStore(editDateStore) -> 触发 flow 的emit -> 传递到state -> 触发compose重组
 *
 * 添加一个属性时，记得修改对应flow的map中的新建实例的传参
 */
class SettingViewModel : ViewModel() {

    // 一般设置
    var resolutionText by mutableStateOf("")
        private set
    //FIXME 这样如果在别处修改了，而不是在设置界面修改的话，这里不会更新。Composable中接收到的数据还是旧数据吧？
    /** 当前[Consts.rootfsAllDir]下的的rootfs列表 不包括[Consts.rootfsCurrDir] */
    val rootfsList = mutableStateOf(listOf<String>())

//    /** key为[rootfsList]中的名字，value为对应rootfs的当前选择的登陆用户名 */
//    val rootfsUsersCurr = mutableStateOf(mapOf<String, String>())

    /** key为[rootfsList]中的名字，value为对应rootfs的全部可用的登陆用户名 */
    val rootfsUsersOptions = mutableStateOf(mapOf<String, List<String>>())

    val generalFLow = dataStore.data.map { pref ->
        PrefGeneral(
            general_resolution.run { pref[key] ?: default },
            general_shared_ext_path.run { pref[key] ?: default },
            general_rootfs_lang.run { pref[key] ?: default },
            Pref.Local.rootfs_login_user_json.run { pref[key] ?: default },
        )
    }
    val generalState = stateInSimple(PrefGeneral_DEFAULT, generalFLow)

    // proot设置
    val prootFlow = dataStore.data.map { pref ->
        PrefProot(
            proot_bool_options.run { pref[key] ?: default },
            proot_startup_cmd.run { pref[key] ?: default },
        )
    }
    val prootState = stateInSimple(PrefProot_DEFAULT, prootFlow)

    init {
        //部分数据不会自动更新，请在 [updateValuesWhenEnterSettings] 中更新，确保进入设置界面时会获取一次最新的值
    }

    /**
     * 当进入设置界面时，某些数据（例如本地文件列表）可能已经发生变化，但内存中的数据不会更改。所以需要在此时更新这些数据
     */
    fun updateValuesWhenEnterSettings() {
        Log.e(TAG, "updateValuesWhenEnterSettings: 测试一下不执行这个函数的时候是不是不更新")
        viewModelScope.launch {
            resolutionText = general_resolution.get() //resolutionText不随flow更改，初始化先读取一下
            withContext(IO) {
                rootfsList.value = Consts.rootfsAllDir.list()?.toMutableList()?.minus(Consts.rootfsCurrDir.name) ?: listOf()
                rootfsUsersOptions.value = getRootfsUsersOptions()
            }
        }
    }

    /** 点击重置按钮 */
    suspend fun resetSettings() = withContext(IO) {
        dataStore.edit { pref -> Pref.allItems.forEach { item -> pref[item.key] = item.default } }
    }

    /** 点击导入按钮，从本地文件读取json转为用户偏好 */
    suspend fun importSettings(ctx: Context, uri: Uri) = withContext(IO) {
        return@withContext kotlin.runCatching {
            val readResult = Utils.Files.readFromUri(ctx, uri)
            if (readResult.isFailure) throw readResult.exceptionOrNull()!!
            val map = Utils.Pref.deserializeFromJsonToMap(readResult.getOrNull()!!)
            dataStore.edit { preference ->
                for (entry in map) {
                    Pref.allItems.find { it.key.name == entry.key }?.let { item ->
                        preference[item.key] = entry.value
                    }
                }
            }
            return@runCatching
        }
    }

    /** 点击导出按钮. 将当前用户偏好转为json并写入本地文件 */
    suspend fun exportSettings(ctx: Context, uri: Uri) = withContext(Dispatchers.IO) {
        return@withContext kotlin.runCatching {
            val map = dataStore.data.map { preference ->
                val map = mutableMapOf<String, Any>()
                // 导出所有选项的值，如果没有修改过，就返回当前的默认值
                Pref.allItems.forEach { item -> map[item.key.name] = preference[item.key] ?: item.default }
                return@map map
            }.first()
            val json = Utils.Pref.serializeFromMapToJson(map)
            Utils.Files.writeToUri(ctx, uri, json).exceptionOrNull()?.let { throw it }
        }
    }

    suspend fun onChangeProotBoolOptions(option: String, checked: Boolean) = withContext(IO) {
        val newValue = if (checked) prootState.value.boolOptions + option
        else prootState.value.boolOptions - option
        editDateStore(proot_bool_options.key, newValue)
    }

    fun onChangeProotStartupCmd(cmdRaw: String) {
        //换行 -> 空格， 去掉结尾 &, 去掉首尾空格
        editDateStoreAsync(proot_startup_cmd.key, cmdRaw.replace("\n", " ").trim().trimEnd('&').trim())
    }

    private val resolutionRegex = Regex("^(\\d+)(\\D+)(\\d+)$")
    private val resolutionRateLimiter = RateLimiter()

    /** 格式化分辨率。如果格式不对返回null */
    fun formatResolution(text: String): String? = resolutionRegex.matchEntire(text.trim())?.let { matchResult ->
        val (_, w, _, h) = matchResult.groupValues
        if (w.isNotEmpty() && h.isNotEmpty()) "${w}x${h}"
        else null
    }

    /**
     * 分辨率TextField内容变更时的回调。
     * @param forceFormat 如果为 true, 则当传入text不符合格式规范时，将其改为一个符合规范的值并保存
     */
    fun onChangeResolutionText(text: String, forceFormat: Boolean) {
        Log.d(TAG, "onChangeResolutionText: 分辨率更改")
        resolutionText = text

        var formatted = formatResolution(text)
        if (forceFormat && formatted == null) {
            formatted = Pref.general_resolution.default
        }
        //如果符合格式，保存到本地
        if (formatted != null) {
            resolutionText = formatted //这个独立于flow之外所以要手动赋值
            Log.d(TAG, "onChangeResolutionText: 分辨率更改 - 格式正确，保存到本地")
            editDateStoreAsync(general_resolution.key, formatted)
            MainEmuActivity.instance.getPref().displayResolutionCustom.put(formatted)
        }
    }

    /**
     * 添加或删除外部共享目录
     */
    suspend fun onChangeShareExtPath(oldPath: String, newPath: String, action: FuncOnChangeAction) = withContext(Dispatchers.IO) {
        val newList = general_shared_ext_path.get().run {
            when (action) {
                FuncOnChangeAction.EDIT -> minus(oldPath).plus(newPath)
                FuncOnChangeAction.ADD -> plus(newPath)
                FuncOnChangeAction.DEL -> minus(newPath)
            }
        }
        editDateStore(general_shared_ext_path.key, newList)
    }

    /**
     * 修改某rootfs文件夹名，或删除
     * 确保：重命名时，新名称不为 [Consts.rootfsCurrDir] 或其他已有名称。 删除时：[Consts.rootfsCurrDir] 链接不指向该rootfs
     * @return 成功时为空字符串，失败时为失败原因
     */
    suspend fun onChangeRootfsName(oldName: String, newName: String, action: FuncOnChangeAction): String = withContext(Dispatchers.IO) {
        val catchResult = runCatching {
            when (action) {
                FuncOnChangeAction.EDIT -> {
                    if (newName == Consts.rootfsCurrDir.name || File(Consts.rootfsAllDir, newName).exists())
                        return@runCatching "该文件已存在，无法重命名"
                    File(Consts.rootfsAllDir, oldName).renameTo(File(Consts.rootfsAllDir, newName))
                    withContext(Dispatchers.Main) {
                        rootfsList.value = rootfsList.value.minus(oldName).plus(newName)
                        // rootfs重命名后，登陆用户map中的rootfs键也要更新
                        val rootfsToUserMap  = Json.decodeFromString<Map<String,String>>(generalState.value.localRootfsUsersCurr)
                        rootfsToUserMap[oldName]?.let { onChangeRootfsLoginUser(newName, it, rootfsToUserMap) }
                        rootfsUsersOptions.value = getRootfsUsersOptions()
                    }
                }

                FuncOnChangeAction.ADD -> Unit
                FuncOnChangeAction.DEL -> {
                    if (newName == Consts.rootfsCurrDir.name || newName == Consts.rootfsCurrDir.canonicalFile.name)
                        return@runCatching "该Rootfs当前正在运行，无法删除"
                    FileUtils.deleteDirectory(File(Consts.rootfsAllDir, oldName))
                    withContext(Dispatchers.Main) {
                        rootfsList.value = rootfsList.value.minus(oldName)
                    }
                }
            }
            return@runCatching ""
        }
        return@withContext if (catchResult.isSuccess) catchResult.getOrNull()!! else catchResult.exceptionOrNull()!!.stackTraceToString()
    }

    /** 使用[ProotRootfs.getUserInfos]从本地读取rootfs全部可用的用户列表 */
    private fun getRootfsUsersOptions(): Map<String, List<String>> {
        return rootfsList.value.associateWith { rootfs ->
            ProotRootfs.getUserInfos(File(Consts.rootfsAllDir, rootfs)).map { it.name }.sorted()
        }
    }

    suspend fun onChangeRootfsSelect(rootfsName: String) {
        MainEmuActivity.instance.terminalViewModel.stopTerminal()
        Utils.Rootfs.makeCurrent(File(Consts.rootfsAllDir, rootfsName))
        MainEmuActivity.instance.finish()
    }

    suspend fun onChangeRootfsLoginUser(rootfsName: String, userName: String, map: Map<String, String>?=null,) {
        val newJson = (map ?: Json.decodeFromString(generalState.value.localRootfsUsersCurr))
            .filter { rootfsList.value.contains(it.key) }
            .plus(rootfsName to userName)
            .let { Json.encodeToString(it) }
        editDateStore(rootfs_login_user_json.key, newJson)
    }

    fun onChangeRootfsLang(lang: String) = editDateStoreAsync(general_rootfs_lang.key, lang)

}

data class PrefProot(
    /** 只会出现一次且没有附加参数的选项。有全名就尽量使用全名 */
    val boolOptions: Set<String>,
    val startupCmd: String,
)

private val PrefProot_DEFAULT = PrefProot(
    proot_bool_options.default,
    proot_startup_cmd.default,
)

data class PrefGeneral(
    val resolution: String,
    val sharedExtPath: Set<String>,
    val rootfsLang: String,
    val localRootfsUsersCurr: String,
)

private val PrefGeneral_DEFAULT = PrefGeneral(
    general_resolution.default,
    general_shared_ext_path.default,
    general_rootfs_lang.default,
    Pref.Local.rootfs_login_user_json.default,
)

/** 顶部操作按钮类型 */
sealed interface SettingAction {
    data object RESET : SettingAction
    data object IMPORT : SettingAction
    data object EXPORT : SettingAction
}