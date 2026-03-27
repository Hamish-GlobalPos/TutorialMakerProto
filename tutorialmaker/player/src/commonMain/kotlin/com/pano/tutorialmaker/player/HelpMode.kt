package com.pano.tutorialmaker.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.StepMode
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

/**
 * Help Mode overlay. When enabled, shows hint indicators on all tagged elements
 * that have tutorial steps. Tapping one shows its spotlight + tooltip.
 * Tap detection is handled by TutorialMaker via PointerEventPass.Initial on its root Box,
 * so touches always pass through to the app content below.
 */
@Composable
fun HelpMode(
    tagStepMap: Map<String, TutorialStep>,
    enabled: Boolean,
    activeStep: TutorialStep?,
    activeRect: Rect?,
    onDismissStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val density = LocalDensity.current

    if (activeStep != null && activeRect != null) {
        val step = activeStep
        val rect = activeRect
        val paddingPx = with(density) { step.spotlightPaddingDp.dp.toPx() }

        Box(modifier = modifier.fillMaxSize()) {
            SpotlightOverlay(
                targetRect = rect,
                shape = step.spotlightShape,
                paddingPx = paddingPx,
                onScrimClick = { onDismissStep() },
                onTargetClick = { onDismissStep() }
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
                    onNext = { onDismissStep() },
                    onBack = {},
                    onSkip = { onDismissStep() }
                )
            }
        }
    } else {
        // Draw hint dots only — no pointerInput, tap detection lives in TutorialMaker's root Box
        Canvas(modifier = modifier.fillMaxSize()) {
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
