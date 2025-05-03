package org.github.ewt45.winemulator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.Consts.Pref.default_proot_bool_options
import org.github.ewt45.winemulator.Consts.Pref.default_proot_startup_cmd
import org.github.ewt45.winemulator.Consts.Pref.key_proot_bool_options
import org.github.ewt45.winemulator.Consts.Pref.key_proot_startup_cmd
import org.github.ewt45.winemulator.Utils.Ui.editDateStore
import org.github.ewt45.winemulator.Utils.Ui.stateInSimple
import org.github.ewt45.winemulator.dataStore




data class PrefProot(
    /** 只会出现一次且没有附加参数的选项。有全名就尽量使用全名 */
    val proot_bool_options: Set<String> = default_proot_bool_options,
    val proot_startup_cmd: String = default_proot_startup_cmd
)

class SettingViewModel : ViewModel() {
    val prootFlow = dataStore.data.map { pref ->
        PrefProot(
            pref[key_proot_bool_options] ?: default_proot_bool_options,
            pref[key_proot_startup_cmd] ?: default_proot_startup_cmd,
        )
    }
    val prootState = stateInSimple(PrefProot(), prootFlow)

    fun onChangeProotBoolOptions(option: String, checked: Boolean) {
        val newValue = if (checked) prootState.value.proot_bool_options.plus(option)
        else prootState.value.proot_bool_options.minus(option)
        editDateStore(key_proot_bool_options, newValue)
    }

    fun onChangeProotStartupCmd(cmdRaw: String) {
        //换行 -> 空格， 去掉结尾 &, 去掉首尾空格
        editDateStore(key_proot_startup_cmd, cmdRaw.replace("\n", " ").trim().trimEnd('&').trim())
    }

}