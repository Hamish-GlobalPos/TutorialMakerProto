package com.pano.tutorialmaker.tagging

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

fun Modifier.tutorialScrollContainer(tag: String, scrollState: ScrollState): Modifier = composed {
    DisposableEffect(tag) {
        TutorialTagRegistry.registerScrollContainer(tag, scrollState)
        onDispose { TutorialTagRegistry.unregisterScrollContainer(tag) }
    }
    onGloballyPositioned { coordinates ->
        TutorialTagRegistry.updateScrollContainerBounds(tag, coordinates.boundsInRoot())
    }
}
