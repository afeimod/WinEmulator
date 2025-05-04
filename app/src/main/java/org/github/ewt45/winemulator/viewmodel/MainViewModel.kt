package org.github.ewt45.winemulator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 用于 MainViewModel 的state
 */
data class MainUiState(
    /** 对话框类型 */
    val dialogType: DialogType = DialogType.NONE,
    /** 对话框内容 */
    val msg: String = "",

)

//TODO 把这个BLOCK和CONFIRM添加数据，改成uistate,参考nowinandroid使用sealed
sealed interface DialogType {
    /** 阻塞，用户无法手动关闭。执行某一操作后自动关闭 */
    data object BLOCK:DialogType
    /** 确认，显示一条消息，用户可点击确认/取消按钮 */
    data object CONFIRM:DialogType
    /** 不显示对话框 */
    object NONE:DialogType
}

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    //用法：起始位置赋值为CompletableDeferred()，然后执行.await()等待。 结束位置调用.complete()，然后起始位置那里.await()会返回
    private var dialogDeferred: CompletableDeferred<Result<Boolean>>? = null


//    /**
//     * 显示阻塞对话框. 该对话框无法关闭，用于执行一些操作，等操作完成后自动关闭
//     * @param action 要执行的操作。在viewModelScope中运行
//     */
//    fun showBlockDialog(msg: String = "加载中，请稍等", action: (suspend () -> Unit)) {
//        _uiState.update { it.copy(blockDialog = true, blockDialogMsg = msg) }
//        viewModelScope.launch {
//            action()
//            closeBlockDialog()
//        }
//    }

    /**
     * 显示阻塞对话框. 该对话框无法关闭，用于执行一些操作，等操作完成后自动关闭
     * @param action 要执行的操作。在viewModelScope中运行
     * @return 返回一个Result success代表操作执行成功，failure代表失败
     */
    suspend fun showBlockDialog(msg: String = "加载中，请稍等", action: (suspend () -> Unit)):Result<Unit> {
        //更新state,显示dialog
        _uiState.update { it.copy(dialogType = DialogType.BLOCK, msg = msg) }
        //执行action
        val result = kotlin.runCatching { action() }
        _uiState.update { it.copy(dialogType = DialogType.NONE) }
        return result
    }

    /**
     * 显示一个确认对话框。
     * 该函数会一直阻塞到 用户点击确认或取消关闭对话框 为止。
     * 对话框关闭时，返回Result为true代表点击确认，false代表点击取消。
     */
    suspend fun showConfirmDialog(msg: String = "您确定吗？"):Result<Boolean>  {
        dialogDeferred = CompletableDeferred()
        _uiState.update { it.copy(dialogType = DialogType.CONFIRM, msg = msg)  }
        val result = dialogDeferred!!.await()
        _uiState.update { it.copy(dialogType = DialogType.NONE) }
        return result
    }

    /**
     * 关闭确认对话框。
     * @param confirm 为true表示用户点击了确认按钮，为false表示点击了取消按钮
     */
    fun closeConfirmDialog(confirm:Boolean = true) {
        dialogDeferred?.complete(Result.success(confirm))
    }

}