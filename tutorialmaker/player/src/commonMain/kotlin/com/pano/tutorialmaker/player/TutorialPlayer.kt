package com.pano.tutorialmaker.player

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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

    if (!state.isActive) {
        onComplete()
        return
    }

    val step = state.currentStep ?: return

    val resolvedRect = TutorialTagRegistry.resolve(step.target, density)
        ?: Rect(0f, 0f, 0f, 0f)

    val animatedRect by animateRectAsState(
        targetValue = resolvedRect,
        animationSpec = tween(durationMillis = 300),
        label = "spotlight_rect"
    )

    val paddingPx = with(density) { step.spotlightPaddingDp.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        SpotlightOverlay(
            targetRect = animatedRect,
            shape = step.spotlightShape,
            paddingPx = paddingPx,
            onScrimClick = { /* consume tap */ },
            onTargetClick = {
                if (step.dismissOnTargetClick) {
                    state.next()
                }
            }
        )

        TutorialTextBubble(
            text = step.text,
            textPosition = step.textPosition,
            targetRect = animatedRect,
            stepIndex = state.flatStepIndex,
            totalSteps = state.totalSteps,
            textOffsetXDp = step.textOffsetXDp,
            textOffsetYDp = step.textOffsetYDp,
            onNext = {
                if (state.flatStepIndex >= state.totalSteps - 1) {
                    state.skip()
                } else {
                    state.next()
                }
            },
            onBack = { state.previous() },
            onSkip = { state.skip() }
        )
    }
}
