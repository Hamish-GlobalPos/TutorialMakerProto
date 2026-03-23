package com.pano.tutorialmaker.editor.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pano.tutorialmaker.model.TextPosition
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.player.TutorialTextBubble

@Composable
fun HelpTextTool(
    step: TutorialStep,
    targetRect: Rect,
    stepIndex: Int,
    totalSteps: Int,
    onStepChanged: (TutorialStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var dragOffsetX by remember(step.id) { mutableStateOf(step.textOffsetXDp) }
    var dragOffsetY by remember(step.id) { mutableStateOf(step.textOffsetYDp) }

    Box(modifier = modifier.fillMaxSize()) {
        // Live preview of the text bubble
        TutorialTextBubble(
            text = step.text.ifEmpty { "(empty text)" },
            textPosition = step.textPosition,
            targetRect = targetRect,
            stepIndex = stepIndex,
            totalSteps = totalSteps,
            textOffsetXDp = dragOffsetX,
            textOffsetYDp = dragOffsetY,
            onNext = {},
            onBack = {},
            onSkip = {}
        )

        // Drag overlay + guidelines canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(step.id) {
                    detectDragGestures(
                        onDragEnd = {
                            onStepChanged(
                                step.copy(
                                    textOffsetXDp = dragOffsetX,
                                    textOffsetYDp = dragOffsetY
                                )
                            )
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        with(density) {
                            dragOffsetX += dragAmount.x.toDp().value
                            dragOffsetY += dragAmount.y.toDp().value
                        }
                    }
                }
        ) {
            val dashedStroke = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Draw a guideline from target center to approximate bubble anchor
            val bubblePadding = with(density) { 16.dp.toPx() }
            val anchorPoint = with(density) {
                when (step.textPosition) {
                    TextPosition.ABOVE -> Offset(
                        targetRect.left + dragOffsetX.dp.toPx(),
                        targetRect.top - bubblePadding + dragOffsetY.dp.toPx()
                    )
                    TextPosition.BELOW -> Offset(
                        targetRect.left + dragOffsetX.dp.toPx(),
                        targetRect.bottom + bubblePadding + dragOffsetY.dp.toPx()
                    )
                    TextPosition.LEFT -> Offset(
                        targetRect.left - bubblePadding + dragOffsetX.dp.toPx(),
                        targetRect.center.y + dragOffsetY.dp.toPx()
                    )
                    TextPosition.RIGHT -> Offset(
                        targetRect.right + bubblePadding + dragOffsetX.dp.toPx(),
                        targetRect.center.y + dragOffsetY.dp.toPx()
                    )
                    TextPosition.CENTER -> Offset(
                        targetRect.center.x + dragOffsetX.dp.toPx(),
                        targetRect.center.y + dragOffsetY.dp.toPx()
                    )
                }
            }

            // Guideline from target center to bubble anchor
            drawLine(
                color = Color.Magenta.copy(alpha = 0.5f),
                start = targetRect.center,
                end = anchorPoint,
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Small dot at anchor point
            drawCircle(
                color = Color.Magenta.copy(alpha = 0.7f),
                radius = 4f,
                center = anchorPoint
            )

            // Offset label
            val label = textMeasurer.measure(
                text = "offset: (${dragOffsetX.toInt()}, ${dragOffsetY.toInt()}) dp",
                style = TextStyle(fontSize = 9.sp, color = Color.Magenta.copy(alpha = 0.8f))
            )
            drawText(
                textLayoutResult = label,
                topLeft = Offset(anchorPoint.x + 8f, anchorPoint.y - label.size.height - 4f)
            )
        }
    }
}
