package com.pano.tutorialmaker.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.SpotlightShape
import kotlin.math.max

@Composable
fun SpotlightOverlay(
    targetRect: Rect,
    shape: SpotlightShape,
    paddingPx: Float,
    scrimColor: Color = Color.Black.copy(alpha = 0.7f),
    passTargetTaps: Boolean = false,
    passAllTaps: Boolean = false,
    onScrimClick: () -> Unit = {},
    onTargetClick: () -> Unit = {}
) {
    val paddedRect = Rect(
        left = targetRect.left - paddingPx,
        top = targetRect.top - paddingPx,
        right = targetRect.right + paddingPx,
        bottom = targetRect.bottom + paddingPx
    )

    // Visual scrim with hole
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRect(color = scrimColor)
        when (shape) {
            SpotlightShape.CIRCLE -> {
                val radius = max(paddedRect.width, paddedRect.height) / 2f
                drawCircle(
                    color = Color.Black, radius = radius,
                    center = paddedRect.center, blendMode = BlendMode.DstOut
                )
            }
            SpotlightShape.RECT -> {
                drawRect(
                    color = Color.Black, topLeft = paddedRect.topLeft,
                    size = paddedRect.size, blendMode = BlendMode.DstOut
                )
            }
            SpotlightShape.ROUNDED_RECT -> {
                drawRoundRect(
                    color = Color.Black, topLeft = paddedRect.topLeft,
                    size = paddedRect.size, cornerRadius = CornerRadius(12f, 12f),
                    blendMode = BlendMode.DstOut
                )
            }
        }
    }

    if (passAllTaps) {
        // Scroll mode: visual scrim only, all touches pass through so user can scroll freely
        return
    }

    if (passTargetTaps) {
        // Four scrim-blocking regions around the hole.
        // The hole itself has NO composable — taps fall through to the app.
        val density = LocalDensity.current
        val scrimTapMod = Modifier.pointerInput(Unit) {
            detectTapGestures { onScrimClick() }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()

            with(density) {
                // Top region: full width, from top to target top
                if (paddedRect.top > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(paddedRect.top.toDp())
                            .then(scrimTapMod)
                    )
                }

                // Bottom region: full width, from target bottom to screen bottom
                val bottomHeight = screenH - paddedRect.bottom
                if (bottomHeight > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(y = paddedRect.bottom.toDp())
                            .fillMaxWidth()
                            .height(bottomHeight.toDp())
                            .then(scrimTapMod)
                    )
                }

                // Left region: from target top to target bottom, left edge to target left
                if (paddedRect.left > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(y = paddedRect.top.toDp())
                            .width(paddedRect.left.toDp())
                            .height(paddedRect.height.toDp())
                            .then(scrimTapMod)
                    )
                }

                // Right region: from target top to target bottom, target right to screen right
                val rightWidth = screenW - paddedRect.right
                if (rightWidth > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = paddedRect.right.toDp(),
                                y = paddedRect.top.toDp()
                            )
                            .width(rightWidth.toDp())
                            .height(paddedRect.height.toDp())
                            .then(scrimTapMod)
                    )
                }
            }

            // Invisible observer on the target — uses Final pass so the button gets it first
            with(density) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = paddedRect.left.toDp(),
                            y = paddedRect.top.toDp()
                        )
                        .width(paddedRect.width.toDp())
                        .height(paddedRect.height.toDp())
                )
                // No pointerInput here — taps go straight through to the button
                // onTargetClick is fired by the tutorialTag modifier's observer
            }
        }

        // The tutorialTag pointerInput observer on the button will fire,
        // and we register a callback to advance the tutorial
        val targetTag = targetRect.hashCode().toString() // Not ideal but we handle it in TutorialPlayer
    } else {
        // Default: block all taps
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(paddedRect) {
                    detectTapGestures { offset ->
                        if (paddedRect.contains(offset)) {
                            onTargetClick()
                        } else {
                            onScrimClick()
                        }
                    }
                }
        )
    }
}
