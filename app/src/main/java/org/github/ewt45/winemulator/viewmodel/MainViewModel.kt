package org.github.ewt45.winemulator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.MainUiState

class MainViewModel:ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()


    /** 显示阻塞对话框. 在viewModelScope中执行action, 执行完毕时自动关闭对话框 */
    fun showBlockDialog(msg: String="加载中，请稍等", action:suspend ()->Unit) {
        _uiState.update { it.copy(blockDialog = true, blockDialogMsg = msg) }
        viewModelScope.launch {
            action()
            closeBlockDialog()
        }
    }

    /** 显示阻塞对话框 */
    fun showBlockDialog(msg: String="加载中，请稍等", ) {
        _uiState.update { it.copy(blockDialog = true, blockDialogMsg = msg) }
    }

    /**
     * 关闭阻塞对话框
     * 目前尚未实现完整机制，所以请确保同一时间仅有一处位置显示/关闭对话框。如果有第二个位置，可能导致第一个未完成时就关闭对话框
     */
    fun closeBlockDialog() {
        _uiState.update { it.copy(blockDialog = false) }
    }
    
}