package org.github.ewt45.winemulator.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class PrepareStageViewModel:ViewModel() {
    val isNoRootfs = mutableStateOf(true)
    fun setNoRootfs(noRootfs: Boolean) {
        isNoRootfs.value = noRootfs
    }

}