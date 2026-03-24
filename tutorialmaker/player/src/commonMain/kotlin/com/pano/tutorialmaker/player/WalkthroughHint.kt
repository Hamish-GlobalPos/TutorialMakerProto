package com.pano.tutorialmaker.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.TextPosition
import kotlin.math.roundToInt

@Composable
fun WalkthroughHint(
    text: String,
    textPosition: TextPosition,
    targetRect: Rect,
    textOffsetXDp: Float,
    textOffsetYDp: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bubblePadding = 16.dp

    val rawOffsetX: Float
    val rawOffsetY: Float

    with(density) {
        when (textPosition) {
            TextPosition.ABOVE -> {
                rawOffsetX = targetRect.left + textOffsetXDp.dp.toPx()
                rawOffsetY = targetRect.top - bubblePadding.toPx() + textOffsetYDp.dp.toPx()
            }
            TextPosition.BELOW -> {
                rawOffsetX = targetRect.left + textOffsetXDp.dp.toPx()
                rawOffsetY = targetRect.bottom + bubblePadding.toPx() + textOffsetYDp.dp.toPx()
            }
            TextPosition.LEFT -> {
                rawOffsetX = targetRect.left - bubblePadding.toPx() + textOffsetXDp.dp.toPx()
                rawOffsetY = targetRect.center.y + textOffsetYDp.dp.toPx()
            }
            TextPosition.RIGHT -> {
                rawOffsetX = targetRect.right + bubblePadding.toPx() + textOffsetXDp.dp.toPx()
                rawOffsetY = targetRect.center.y + textOffsetYDp.dp.toPx()
            }
            TextPosition.CENTER -> {
                rawOffsetX = targetRect.center.x + textOffsetXDp.dp.toPx()
                rawOffsetY = targetRect.center.y + textOffsetYDp.dp.toPx()
            }
        }
    }

    var bubbleSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier = modifier) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val margin = with(density) { 8.dp.toPx() }

        Card(
            modifier = Modifier
                .offset {
                    val clampedX = rawOffsetX.coerceIn(margin, (screenW - bubbleSize.width - margin).coerceAtLeast(margin))
                    val clampedY = rawOffsetY.coerceIn(margin, (screenH - bubbleSize.height - margin).coerceAtLeast(margin))
                    IntOffset(clampedX.roundToInt(), clampedY.roundToInt())
                }
                .widthIn(max = 280.dp)
                .onSizeChanged { bubbleSize = it },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
