package org.github.ewt45.winemulator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.github.ewt45.winemulator.Consts
import org.github.ewt45.winemulator.MainEmuApplication
import org.github.ewt45.winemulator.Utils
import org.github.ewt45.winemulator.Utils.Permissions.isGranted
import org.github.ewt45.winemulator.Utils.Ui.editDateStore
import org.github.ewt45.winemulator.Utils.Ui.editDateStoreAsync
import org.github.ewt45.winemulator.permissions.RequiredPermissions

data class PrepareUiState(
    val loading: Boolean = true,
    val unGrantedPermissions: List<RequiredPermissions> = listOf(),
    val skipPermissions: Boolean = false,
    val noRootfs: Boolean = false,
    val forceNoRootfs: Boolean = false,
    val shouldRestart: Boolean = false,
) {
    /** 准备完成。若返回true则应离开prepareScreen 进入主界面 */
    val isPrepareFinished:Boolean
        get() = !loading
                && (skipPermissions || unGrantedPermissions.isEmpty())
                && !noRootfs && !forceNoRootfs
}

class PrepareViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PrepareUiState(loading = true))
    val uiState: StateFlow<PrepareUiState> = _uiState.asStateFlow()

    /** 进入preparescreen之后执行此函数 检查是否有必要显示 */
    suspend fun updateState() = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(loading = true) }
        val unGrantedList = RequiredPermissions.getUnGrantedList()
        _uiState.update {
            it.copy(
                loading = false,
                unGrantedPermissions = unGrantedList,
                skipPermissions = Consts.Pref.Local.skip_permissions.get(),
                noRootfs = Utils.Rootfs.haveNoRootfs()
            )
        }
    }

    /** 用户授权某个权限后，修改未授予权限列表 */
    fun onGrantedPermission(permission: RequiredPermissions) {
        _uiState.update { it.copy(unGrantedPermissions = it.unGrantedPermissions.minus(permission)) }
    }

    /** 用户跳过权限申请 */
    fun onSkipPermissions() {
        editDateStoreAsync(Consts.Pref.Local.skip_permissions.key, true)
        _uiState.update { it.copy(skipPermissions = true) }
    }

    /** 添加rootfs时调用。会将 [PrepareUiState.forceNoRootfs] 设为true, 以跳转到PrepareScreen */
    fun setForceNoRootfs() {
        _uiState.update { it.copy(forceNoRootfs = true) }
    }

    /** rootfs自动提取成功后调用，用于更新状态 */
    fun onRootfsExtracted(rootfsName: String) {
        _uiState.update { it.copy(forceNoRootfs = false, noRootfs = false) }
    }
}