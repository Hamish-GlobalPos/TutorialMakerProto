package com.pano.tutorialmaker.tagging

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.TargetSpec
import com.pano.tutorialmaker.model.TutorialSection

data class ScrollContainerInfo(val scrollState: ScrollState, val bounds: Rect = Rect.Zero)

object TutorialTagRegistry {

    val elements = mutableStateMapOf<String, Rect>()
    val screens = mutableStateMapOf<String, Boolean>()
    val elementTapCallbacks = mutableStateMapOf<String, () -> Unit>()
    val scrollContainers = mutableStateMapOf<String, ScrollContainerInfo>()


    fun registerScrollContainer(tag: String, state: ScrollState) {
        scrollContainers[tag] = ScrollContainerInfo(state)
    }
    fun updateScrollContainerBounds(tag: String, bounds: Rect) {
        val info = scrollContainers[tag] ?: return
        if (info.bounds != bounds) scrollContainers[tag] = info.copy(bounds = bounds)
    }
    fun unregisterScrollContainer(tag: String) { scrollContainers.remove(tag) }

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
        target.tag?.let { tag ->
            elements[tag]?.let { return it }
        }
        return null
    }
}
