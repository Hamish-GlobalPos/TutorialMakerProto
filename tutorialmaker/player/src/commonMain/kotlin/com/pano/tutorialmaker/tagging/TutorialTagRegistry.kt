package com.pano.tutorialmaker.tagging

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.TargetSpec
import com.pano.tutorialmaker.model.TutorialSection

object TutorialTagRegistry {

    val elements = mutableStateMapOf<String, Rect>()
    val screens = mutableStateMapOf<String, Boolean>()
    val elementTapCallbacks = mutableStateMapOf<String, () -> Unit>()

    // Bridge: SectionTrigger requests sections to play, TutorialMaker observes and plays them
    var pendingPlayRequest by mutableStateOf<List<TutorialSection>?>(null)
        private set

    fun requestPlaySections(sections: List<TutorialSection>) {
        pendingPlayRequest = sections
    }

    fun consumePlayRequest(): List<TutorialSection>? {
        val req = pendingPlayRequest
        pendingPlayRequest = null
        return req
    }

    fun register(tag: String, boundsInRoot: Rect) {
        elements[tag] = boundsInRoot
    }

    fun unregister(tag: String) {
        elements.remove(tag)
    }

    fun registerScreen(screenTag: String) {
        screens[screenTag] = true
    }

    fun unregisterScreen(screenTag: String) {
        screens.remove(screenTag)
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
