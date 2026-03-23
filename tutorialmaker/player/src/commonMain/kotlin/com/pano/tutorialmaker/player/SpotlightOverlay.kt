package com.pano.tutorialmaker.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.pano.tutorialmaker.model.SpotlightShape
import kotlin.math.max

@Composable
fun SpotlightOverlay(
    targetRect: Rect,
    shape: SpotlightShape,
    paddingPx: Float,
    scrimColor: Color = Color.Black.copy(alpha = 0.7f),
    onScrimClick: () -> Unit = {},
    onTargetClick: () -> Unit = {}
) {
    val paddedRect = Rect(
        left = targetRect.left - paddingPx,
        top = targetRect.top - paddingPx,
        right = targetRect.right + paddingPx,
        bottom = targetRect.bottom + paddingPx
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput(paddedRect) {
                detectTapGestures { offset ->
                    if (paddedRect.contains(offset)) {
                        onTargetClick()
                    } else {
                        onScrimClick()
                    }
                }
            }
    ) {
        // Draw full-screen scrim
        drawRect(color = scrimColor)

        // Punch hole for spotlight
        when (shape) {
            SpotlightShape.CIRCLE -> {
                val radius = max(paddedRect.width, paddedRect.height) / 2f
                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    center = paddedRect.center,
                    blendMode = BlendMode.DstOut
                )
            }
            SpotlightShape.RECT -> {
                drawRect(
                    color = Color.Black,
                    topLeft = paddedRect.topLeft,
                    size = paddedRect.size,
                    blendMode = BlendMode.DstOut
                )
            }
            SpotlightShape.ROUNDED_RECT -> {
                drawRoundRect(
                    color = Color.Black,
                    topLeft = paddedRect.topLeft,
                    size = paddedRect.size,
                    cornerRadius = CornerRadius(12f, 12f),
                    blendMode = BlendMode.DstOut
                )
            }
        }
    }
}
