package org.github.ewt45.winemulator

/**
 * 用于 MainViewModel 的state
 */
data class MainUiState (
    /** 是否显示一个对话框，不可关闭，用于阻挡用户操作。 */
    val blockDialog: Boolean = false,
    /** block对话框内容 */
    val blockDialogMsg: String = ""
)