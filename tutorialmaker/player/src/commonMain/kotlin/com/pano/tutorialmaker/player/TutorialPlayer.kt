package com.pano.tutorialmaker.player

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.StepMode
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

@Composable
fun TutorialPlayer(
    tutorial: Tutorial,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberTutorialPlayerState(tutorial)
    val density = LocalDensity.current

    LaunchedEffect(state.isActive) {
        if (!state.isActive) {
            onComplete()
        }
    }

    if (!state.isActive) return

    val step = state.currentStep ?: return
    val targetTag = step.target.tag
    val isWalkthrough = step.mode == StepMode.WALKTHROUGH

    val resolvedRect = TutorialTagRegistry.resolve(step.target, density)
    val targetFound = resolvedRect != null
    val targetRect = resolvedRect ?: Rect(0f, 0f, 0f, 0f)

    val animatedRect by animateRectAsState(
        targetValue = targetRect,
        animationSpec = tween(durationMillis = 300),
        label = "spotlight_rect"
    )

    val paddingPx = with(density) { step.spotlightPaddingDp.dp.toPx() }

    // Target not on screen — show a navigation hint instead of a broken spotlight
    if (!targetFound) {
        Box(modifier = modifier.fillMaxSize()) {
            WalkthroughHint(
                text = "Navigate to find: ${targetTag ?: "target"}",
                textPosition = com.pano.tutorialmaker.model.TextPosition.CENTER,
                targetRect = Rect(
                    density.run { 100.dp.toPx() },
                    density.run { 300.dp.toPx() },
                    density.run { 300.dp.toPx() },
                    density.run { 350.dp.toPx() }
                ),
                textOffsetXDp = 0f,
                textOffsetYDp = 0f
            )
        }
        return
    }

    // In walkthrough mode, register a callback on the target tag so when the user
    // taps the actual button (through the hole), we also advance the tutorial
    if (isWalkthrough && targetTag != null) {
        DisposableEffect(targetTag, state.currentStepIndex) {
            val previousCallback = TutorialTagRegistry.elementTapCallbacks[targetTag]
            TutorialTagRegistry.elementTapCallbacks[targetTag] = {
                state.next()
            }
            onDispose {
                // Restore previous callback or remove
                if (previousCallback != null) {
                    TutorialTagRegistry.elementTapCallbacks[targetTag] = previousCallback
                } else {
                    TutorialTagRegistry.elementTapCallbacks.remove(targetTag)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SpotlightOverlay(
            targetRect = animatedRect,
            shape = step.spotlightShape,
            paddingPx = paddingPx,
            passTargetTaps = isWalkthrough,
            onScrimClick = { /* block */ },
            onTargetClick = {
                if (isWalkthrough || step.dismissOnTargetClick) {
                    state.next()
                }
            }
        )

        if (isWalkthrough) {
            WalkthroughHint(
                text = step.text,
                textPosition = step.textPosition,
                targetRect = animatedRect,
                textOffsetXDp = step.textOffsetXDp,
                textOffsetYDp = step.textOffsetYDp
            )
        } else {
            TutorialTextBubble(
                text = step.text,
                textPosition = step.textPosition,
                targetRect = animatedRect,
                stepIndex = state.currentStepIndex,
                totalSteps = state.currentSectionStepCount,
                textOffsetXDp = step.textOffsetXDp,
                textOffsetYDp = step.textOffsetYDp,
                onNext = { state.next() },
                onBack = { state.previous() },
                onSkip = { state.skip() }
            )
        }
    }
}
