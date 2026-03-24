package com.pano.tutorialmaker.tagging

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
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
    }.pointerInput(tag) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Release) {
                    TutorialTagRegistry.elementTapCallbacks[tag]?.invoke()
                }
            }
        }
    }
}
