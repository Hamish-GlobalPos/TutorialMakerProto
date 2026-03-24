package com.pano.tutorialmaker.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pano.tutorialmaker.io.TutorialProgressManager
import com.pano.tutorialmaker.model.TriggerType
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

/**
 * Place on each screen to auto-trigger tutorial sections.
 * The actual player lives in TutorialMaker (root level) so it survives navigation.
 * This composable only decides WHEN to trigger — it delegates playing upward.
 */
@Composable
fun SectionTrigger(
    tutorial: Tutorial,
    screenTag: String,
    progressManager: TutorialProgressManager,
    version: Int = 0,
    modifier: Modifier = Modifier
) {
    println("TM_LOG [SectionTrigger] composing: screenTag=$screenTag, version=$version")

    // Register this screen so the editor can see it
    DisposableEffect(screenTag) {
        TutorialTagRegistry.registerScreen(screenTag)
        onDispose { TutorialTagRegistry.unregisterScreen(screenTag) }
    }

    val screenSections = remember(tutorial, screenTag, version) {
        tutorial.sections.filter { it.screenTag == screenTag && it.triggerType == TriggerType.SCREEN }
    }
    val elementSections = remember(tutorial, screenTag, version) {
        tutorial.sections.filter { it.screenTag == screenTag && it.triggerType == TriggerType.ELEMENT }
    }

    // Auto-trigger screen sections on first visit
    LaunchedEffect(screenTag, version) {
        val queue = mutableListOf<com.pano.tutorialmaker.model.TutorialSection>()
        for (section in screenSections) {
            if (!progressManager.isSectionCompleted(tutorial.id, section.id)) {
                queue.add(section)
                progressManager.markSectionCompleted(tutorial.id, section.id)
            }
        }
        if (queue.isNotEmpty()) {
            TutorialTagRegistry.requestPlaySections(queue)
        }
    }

    // Element triggers
    val pendingElements = remember(elementSections, tutorial, version) {
        elementSections
            .filter { !progressManager.isSectionCompleted(tutorial.id, it.id) }
            .mapNotNull { section ->
                section.elementTag?.let { tag -> tag to section }
            }
            .toMap()
    }

    DisposableEffect(pendingElements) {
        for ((tag, section) in pendingElements) {
            TutorialTagRegistry.elementTapCallbacks[tag] = {
                println("TM_LOG [SectionTrigger] element tapped: $tag -> ${section.id}")
                progressManager.markSectionCompleted(tutorial.id, section.id)
                TutorialTagRegistry.requestPlaySections(listOf(section))
            }
        }
        onDispose {
            for (tag in pendingElements.keys) {
                TutorialTagRegistry.elementTapCallbacks.remove(tag)
            }
        }
    }

    if (pendingElements.isNotEmpty()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for ((tag, _) in pendingElements) {
                val rect = TutorialTagRegistry.elements[tag] ?: continue
                drawCircle(
                    color = Color(0xFF2196F3).copy(alpha = 0.4f),
                    radius = 6f,
                    center = Offset(rect.right + 2f, rect.top - 2f)
                )
            }
        }
    }
}
