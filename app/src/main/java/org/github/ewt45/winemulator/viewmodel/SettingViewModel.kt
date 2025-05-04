package org.github.ewt45.winemulator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.Utils.Ui.editDateStore
import org.github.ewt45.winemulator.Utils.Ui.stateInSimple
import org.github.ewt45.winemulator.dataStore
import org.github.ewt45.winemulator.Consts.Pref
import org.github.ewt45.winemulator.Consts.Pref.proot_bool_options
import org.github.ewt45.winemulator.Consts.Pref.proot_startup_cmd


data class PrefProot(
    /** 只会出现一次且没有附加参数的选项。有全名就尽量使用全名 */
    val proot_bool_options: Set<String> = Pref.proot_bool_options.default,
    val proot_startup_cmd: String = Pref.proot_startup_cmd.default
)

/** 顶部操作按钮类型 */
sealed interface SettingAction {
    object RESET: SettingAction
    object IMPORT: SettingAction
    object EXPORT: SettingAction
}

class SettingViewModel : ViewModel() {
    val prootFlow = dataStore.data.map { pref ->
        PrefProot(
            pref[proot_bool_options.key] ?: proot_bool_options.default,
            pref[proot_startup_cmd.key] ?: proot_startup_cmd.default,
        )
    }
    val prootState = stateInSimple(PrefProot(), prootFlow)

    /**
     * 点击顶部操作按钮时
     */
    fun onActionClick(action: SettingAction) {
        when(action) {
            SettingAction.RESET -> {

            }

            SettingAction.EXPORT -> TODO()
            SettingAction.IMPORT -> TODO()
        }
    }

    fun onChangeProotBoolOptions(option: String, checked: Boolean) {
        val newValue = if (checked) prootState.value.proot_bool_options.plus(option)
        else prootState.value.proot_bool_options.minus(option)
        editDateStore(proot_bool_options.key, newValue)
    }

    fun onChangeProotStartupCmd(cmdRaw: String) {
        //换行 -> 空格， 去掉结尾 &, 去掉首尾空格
        editDateStore(proot_startup_cmd.key, cmdRaw.replace("\n", " ").trim().trimEnd('&').trim())
    }


}