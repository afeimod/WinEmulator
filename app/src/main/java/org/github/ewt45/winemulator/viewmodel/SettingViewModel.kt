package org.github.ewt45.winemulator.viewmodel

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.github.ewt45.winemulator.MainUiState
import androidx.core.content.edit


private val key_proot_bool_options = "_proot_bool_options"
private val default_proot_bool_options =
    setOf("--root-id", "-L", "--link2symlink", "--kill-on-exit")

data class PrefProot(
    /** 只会出现一次且没有附加参数的选项。有全名就尽量使用全名 */
    val proot_bool_options: Set<String> = default_proot_bool_options
) {

}

class SettingViewModel : ViewModel() {
    private lateinit var pref: SharedPreferences
    private val _prootState = MutableStateFlow(PrefProot())
    val prootState: StateFlow<PrefProot> = _prootState.asStateFlow()

    fun initPref(ctx: Context) {
        pref = ctx.getSharedPreferences("settings", MODE_PRIVATE)
        _prootState.update {
            it.copy(
                proot_bool_options = pref.getStringSet(
                    key_proot_bool_options,
                    default_proot_bool_options
                )!!
            )
        }
    }

    fun savePref() {
        pref.edit(true) { putStringSet(key_proot_bool_options, _prootState.value.proot_bool_options) }
    }

    fun onChangedProotBoolOptions(option: String, checked: Boolean) {
        _prootState.update {
            it.copy(
                proot_bool_options =
                    if (checked) it.proot_bool_options.plus(option)
                    else it.proot_bool_options.minus(option)
            )
        }
    }

}