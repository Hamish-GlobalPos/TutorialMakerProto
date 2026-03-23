package com.pano.tutorialmaker.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.TextPosition
import kotlin.math.roundToInt

@Composable
fun TutorialTextBubble(
    text: String,
    textPosition: TextPosition,
    targetRect: Rect,
    stepIndex: Int,
    totalSteps: Int,
    textOffsetXDp: Float,
    textOffsetYDp: Float,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bubblePadding = 16.dp

    val offsetX: Float
    val offsetY: Float
    val alignment: Alignment

    with(density) {
        when (textPosition) {
            TextPosition.ABOVE -> {
                offsetX = targetRect.left + textOffsetXDp.dp.toPx()
                offsetY = targetRect.top - bubblePadding.toPx() + textOffsetYDp.dp.toPx()
                alignment = Alignment.BottomStart
            }
            TextPosition.BELOW -> {
                offsetX = targetRect.left + textOffsetXDp.dp.toPx()
                offsetY = targetRect.bottom + bubblePadding.toPx() + textOffsetYDp.dp.toPx()
                alignment = Alignment.TopStart
            }
            TextPosition.LEFT -> {
                offsetX = targetRect.left - bubblePadding.toPx() + textOffsetXDp.dp.toPx()
                offsetY = targetRect.center.y + textOffsetYDp.dp.toPx()
                alignment = Alignment.CenterEnd
            }
            TextPosition.RIGHT -> {
                offsetX = targetRect.right + bubblePadding.toPx() + textOffsetXDp.dp.toPx()
                offsetY = targetRect.center.y + textOffsetYDp.dp.toPx()
                alignment = Alignment.CenterStart
            }
            TextPosition.CENTER -> {
                offsetX = targetRect.center.x + textOffsetXDp.dp.toPx()
                offsetY = targetRect.center.y + textOffsetYDp.dp.toPx()
                alignment = Alignment.Center
            }
        }
    }

    Card(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .widthIn(max = 300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stepIndex + 1}/$totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                    if (stepIndex > 0) {
                        TextButton(onClick = onBack) {
                            Text("Back")
                        }
                    }
                    TextButton(onClick = onNext) {
                        Text(if (stepIndex < totalSteps - 1) "Next" else "Done")
                    }
                }
            }
        }
    }
}
