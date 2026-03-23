package com.pano.tutorialmaker.tagging

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

fun Modifier.tutorialTag(tag: String): Modifier = composed {
    DisposableEffect(tag) {
        onDispose {
            TutorialTagRegistry.unregister(tag)
        }
    }

    onGloballyPositioned { coordinates ->
        TutorialTagRegistry.register(tag, coordinates.boundsInRoot())
    }
}
