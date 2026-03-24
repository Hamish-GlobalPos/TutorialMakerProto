package com.pano.tutorialmaker.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.StepMode
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

/**
 * Help Mode overlay. When enabled, shows hint indicators on all tagged elements
 * that have tutorial steps. Tapping one shows its spotlight + tooltip.
 */
@Composable
fun HelpMode(
    tutorial: Tutorial,
    enabled: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val density = LocalDensity.current

    // Build tag -> step lookup from all sections
    val tagStepMap = remember(tutorial) {
        val map = mutableMapOf<String, TutorialStep>()
        for (section in tutorial.sections) {
            for (step in section.steps) {
                val tag = step.target.tag ?: continue
                if (tag !in map) {
                    map[tag] = step
                }
            }
        }
        map
    }

    var activeStep by remember { mutableStateOf<TutorialStep?>(null) }
    var activeRect by remember { mutableStateOf<Rect?>(null) }

    if (activeStep != null && activeRect != null) {
        // Show spotlight + tooltip for the tapped element
        val step = activeStep!!
        val rect = activeRect!!
        val paddingPx = with(density) { step.spotlightPaddingDp.dp.toPx() }

        Box(modifier = modifier.fillMaxSize()) {
            SpotlightOverlay(
                targetRect = rect,
                shape = step.spotlightShape,
                paddingPx = paddingPx,
                onScrimClick = {
                    activeStep = null
                    activeRect = null
                },
                onTargetClick = {
                    activeStep = null
                    activeRect = null
                }
            )

            if (step.mode == StepMode.WALKTHROUGH) {
                WalkthroughHint(
                    text = step.text,
                    textPosition = step.textPosition,
                    targetRect = rect,
                    textOffsetXDp = step.textOffsetXDp,
                    textOffsetYDp = step.textOffsetYDp
                )
            } else {
                TutorialTextBubble(
                    text = step.text,
                    textPosition = step.textPosition,
                    targetRect = rect,
                    stepIndex = 0,
                    totalSteps = 1,
                    textOffsetXDp = step.textOffsetXDp,
                    textOffsetYDp = step.textOffsetYDp,
                    onNext = {
                        activeStep = null
                        activeRect = null
                    },
                    onBack = {},
                    onSkip = {
                        activeStep = null
                        activeRect = null
                    }
                )
            }
        }
    } else {
        // Show hint indicators on all tagged elements with steps
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(tagStepMap) {
                    detectTapGestures { offset ->
                        // Find which tagged element was tapped
                        for ((tag, rect) in TutorialTagRegistry.elements) {
                            if (rect.contains(offset) && tag in tagStepMap) {
                                activeStep = tagStepMap[tag]
                                activeRect = rect
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            // Draw hint dots on elements that have tooltips
            for ((tag, rect) in TutorialTagRegistry.elements) {
                if (tag in tagStepMap) {
                    drawCircle(
                        color = Color.Blue.copy(alpha = 0.6f),
                        radius = 10f,
                        center = Offset(rect.right + 6f, rect.top - 6f)
                    )
                }
            }
        }
    }
}
