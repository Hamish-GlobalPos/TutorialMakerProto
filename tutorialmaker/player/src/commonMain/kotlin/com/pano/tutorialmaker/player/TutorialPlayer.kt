package com.pano.tutorialmaker.player

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.StepMode
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.tagging.TutorialTagRegistry
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

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
    val isScroll = step.mode == StepMode.SCROLL

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()

        // In scroll mode the spotlight uses viewport-relative fractions so it stays
        // in the same relative position regardless of window size.
        // The tag is used only for trigger line detection.
        val resolvedRect = when {
            isScroll -> {
                val t = step.target
                if (t.fallbackXFrac != null && t.fallbackYFrac != null &&
                    t.fallbackWidthFrac != null && t.fallbackHeightFrac != null) {
                    Rect(
                        t.fallbackXFrac * viewportWidthPx,
                        t.fallbackYFrac * viewportHeightPx,
                        (t.fallbackXFrac + t.fallbackWidthFrac) * viewportWidthPx,
                        (t.fallbackYFrac + t.fallbackHeightFrac) * viewportHeightPx
                    )
                } else null
            }
            else -> TutorialTagRegistry.resolve(step.target, density)
        }

        val targetFound = resolvedRect != null
        val targetRect = resolvedRect ?: Rect(0f, 0f, 0f, 0f)

        val animatedRect by animateRectAsState(
            targetValue = targetRect,
            animationSpec = tween(durationMillis = 300),
            label = "spotlight_rect"
        )

        val paddingPx = with(density) { step.spotlightPaddingDp.dp.toPx() }

        LaunchedEffect(state.flatStepIndex) {
            val trigger = step.scrollTrigger ?: return@LaunchedEffect
            val tag = step.target.tag ?: return@LaunchedEffect

            // Phase 1: wait until element is on the "needs scrolling" side of at least one line
            snapshotFlow { TutorialTagRegistry.elements[tag] }
                .filterNotNull()
                .filter { rect ->
                    trigger.yFraction?.let { rect.center.y > it * viewportHeightPx } == true ||
                    trigger.xFraction?.let { rect.center.x > it * viewportWidthPx } == true
                }
                .first()

            // Phase 2: wait until element crosses all active lines OR scroll is exhausted
            snapshotFlow {
                val rect = TutorialTagRegistry.elements[tag]
                val scrollAtMax = TutorialTagRegistry.scrollContainers.values.any {
                    it.scrollState.maxValue > 0 && it.scrollState.value >= it.scrollState.maxValue
                }
                Pair(rect, scrollAtMax)
            }
                .filter { (rect, scrollAtMax) ->
                    rect ?: return@filter false
                    val crossedLine =
                        (trigger.yFraction == null || rect.center.y <= trigger.yFraction * viewportHeightPx) &&
                        (trigger.xFraction == null || rect.center.x <= trigger.xFraction * viewportWidthPx)
                    crossedLine || scrollAtMax
                }
                .first()

            state.next()
        }

        // Target not on screen — show a navigation hint instead of a broken spotlight
        if (!targetFound) {
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
            return@BoxWithConstraints
        }

        // In walkthrough mode, register a callback on the target tag so when the user
        // taps the actual button (through the hole), we also advance the tutorial
        if (isWalkthrough && targetTag != null) {
            DisposableEffect(targetTag, state.currentStepIndex) {
                val previousCallback = TutorialTagRegistry.elementTapCallbacks[targetTag]
                TutorialTagRegistry.elementTapCallbacks[targetTag] = { state.next() }
                onDispose {
                    if (previousCallback != null) {
                        TutorialTagRegistry.elementTapCallbacks[targetTag] = previousCallback
                    } else {
                        TutorialTagRegistry.elementTapCallbacks.remove(targetTag)
                    }
                }
            }
        }

        step.scrollTrigger?.let { trigger ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                trigger.yFraction?.let { frac ->
                    val y = frac * size.height
                    drawLine(
                        color = Color.Magenta.copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                    )
                }
                trigger.xFraction?.let { frac ->
                    val x = frac * size.width
                    drawLine(
                        color = Color.Magenta.copy(alpha = 0.6f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                    )
                }
            }
        }

        SpotlightOverlay(
            targetRect = animatedRect,
            shape = step.spotlightShape,
            paddingPx = paddingPx,
            passTargetTaps = isWalkthrough,
            passAllTaps = isScroll,
            onScrimClick = { /* block */ },
            onTargetClick = {
                if (isWalkthrough || step.dismissOnTargetClick) {
                    state.next()
                }
            }
        )

        if (isWalkthrough || isScroll) {
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
