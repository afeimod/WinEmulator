package org.github.ewt45.winemulator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

/**
 * 从中心放大缩小
 */
@Composable
fun AnimatedSizeInCenter(visible: Boolean, content: @Composable () -> Unit,) {
    AnimatedVisibility(visible,
        //animationSpec = tween(durationMillis = 300),
        enter = expandIn(expandFrom = Alignment.Center) + fadeIn(),
        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
    ) {
        content()
    }
}