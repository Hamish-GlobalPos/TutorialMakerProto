package com.pano.tutorialmaker.tagging

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.TargetSpec

object TutorialTagRegistry {

    val elements = mutableStateMapOf<String, Rect>()

    fun register(tag: String, boundsInRoot: Rect) {
        elements[tag] = boundsInRoot
    }

    fun unregister(tag: String) {
        elements.remove(tag)
    }

    fun resolve(target: TargetSpec, density: Density): Rect? {
        // Try tag lookup first
        target.tag?.let { tag ->
            elements[tag]?.let { return it }
        }

        // Fallback to dp coordinates
        val x = target.fallbackXDp ?: return null
        val y = target.fallbackYDp ?: return null
        val w = target.fallbackWidthDp ?: return null
        val h = target.fallbackHeightDp ?: return null

        with(density) {
            return Rect(
                left = x.dp.toPx(),
                top = y.dp.toPx(),
                right = (x + w).dp.toPx(),
                bottom = (y + h).dp.toPx()
            )
        }
    }
}
