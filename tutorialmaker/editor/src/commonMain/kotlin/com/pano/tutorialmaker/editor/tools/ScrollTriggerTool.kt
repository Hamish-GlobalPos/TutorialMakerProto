package com.pano.tutorialmaker.editor.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pano.tutorialmaker.model.ScrollTrigger
import com.pano.tutorialmaker.model.TutorialStep
import kotlin.math.abs

private enum class DragAxis { Y, X, NONE }

@Composable
fun ScrollTriggerTool(
    step: TutorialStep,
    onStepChanged: (TutorialStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val trigger = step.scrollTrigger ?: ScrollTrigger(yFraction = 0.5f)
    var yFraction by remember(step.id, trigger.yFraction) { mutableStateOf(trigger.yFraction) }
    var xFraction by remember(step.id, trigger.xFraction) { mutableStateOf(trigger.xFraction) }
    var dragAxis by remember(step.id) { mutableStateOf(DragAxis.NONE) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(step.id) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val hitPx = with(density) { 24.dp.toPx() }
                        val hitY = yFraction?.let { abs(offset.y - it * size.height) < hitPx } ?: false
                        val hitX = xFraction?.let { abs(offset.x - it * size.width) < hitPx } ?: false
                        dragAxis = when {
                            hitY -> DragAxis.Y
                            hitX -> DragAxis.X
                            yFraction != null -> DragAxis.Y  // default to Y if present
                            else -> DragAxis.NONE
                        }
                    },
                    onDragEnd = {
                        onStepChanged(step.copy(scrollTrigger = ScrollTrigger(yFraction, xFraction)))
                        dragAxis = DragAxis.NONE
                    }
                ) { change, dragAmount ->
                    change.consume()
                    when (dragAxis) {
                        DragAxis.Y -> yFraction = yFraction?.plus(dragAmount.y / size.height)?.coerceIn(0f, 1f)
                        DragAxis.X -> xFraction = xFraction?.plus(dragAmount.x / size.width)?.coerceIn(0f, 1f)
                        DragAxis.NONE -> Unit
                    }
                }
            }
    ) {
        val lineColor = Color.Red.copy(alpha = 0.9f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
        val strokeWidth = with(density) { 3.dp.toPx() }

        yFraction?.let { frac ->
            val y = frac * size.height
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )
            val label = textMeasurer.measure(
                text = "Y: ${(frac * 100).toInt()}%",
                style = TextStyle(fontSize = 10.sp, color = lineColor)
            )
            drawText(label, topLeft = Offset(8f, y - label.size.height - 4f))
        }

        xFraction?.let { frac ->
            val x = frac * size.width
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )
            val label = textMeasurer.measure(
                text = "X: ${(frac * 100).toInt()}%",
                style = TextStyle(fontSize = 10.sp, color = lineColor)
            )
            drawText(label, topLeft = Offset(x + 4f, 8f))
        }
    }
}
