package com.pano.tutorialmaker.editor.positioner

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import com.pano.tutorialmaker.model.TargetSpec
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

@Composable
fun SpotlightPositioner(
    currentTarget: TargetSpec,
    onTargetChanged: (TargetSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Draggable fallback rect state (fractions × a nominal 400×800 for this legacy tool)
    var dragRect by remember(currentTarget) {
        with(density) {
            val x = (currentTarget.fallbackXFrac ?: 0.25f) * 400f.dp.toPx()
            val y = (currentTarget.fallbackYFrac ?: 0.12f) * 800f.dp.toPx()
            val w = (currentTarget.fallbackWidthFrac ?: 0.30f) * 400f.dp.toPx()
            val h = (currentTarget.fallbackHeightFrac ?: 0.06f) * 800f.dp.toPx()
            mutableStateOf(Rect(x, y, x + w, y + h))
        }
    }

    val dashedStroke = remember {
        Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        with(density) {
                            onTargetChanged(currentTarget.copy(
                                fallbackXFrac = dragRect.left / (400f.dp.toPx()),
                                fallbackYFrac = dragRect.top / (800f.dp.toPx()),
                                fallbackWidthFrac = dragRect.width / (400f.dp.toPx()),
                                fallbackHeightFrac = dragRect.height / (800f.dp.toPx())
                            ))
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragRect = dragRect.translate(dragAmount)
                }
            }
    ) {
        // Draw outlines of all registered tags
        TutorialTagRegistry.elements.forEach { (tag, rect) ->
            drawRect(
                color = Color.Cyan.copy(alpha = 0.5f),
                topLeft = rect.topLeft,
                size = rect.size,
                style = dashedStroke
            )
            val result = textMeasurer.measure(
                text = tag,
                style = TextStyle(fontSize = 10.sp, color = Color.Cyan)
            )
            drawText(
                textLayoutResult = result,
                topLeft = Offset(rect.left, rect.top - result.size.height)
            )
        }

        // Draw the draggable fallback rect
        drawRect(
            color = Color.Yellow.copy(alpha = 0.6f),
            topLeft = dragRect.topLeft,
            size = dragRect.size,
            style = Stroke(width = 3f)
        )
        // Corner handles
        val handleRadius = 6f
        listOf(
            dragRect.topLeft,
            dragRect.topRight,
            dragRect.bottomLeft,
            dragRect.bottomRight
        ).forEach { corner ->
            drawCircle(
                color = Color.Yellow,
                radius = handleRadius,
                center = corner
            )
        }

        // Label
        val label = textMeasurer.measure(
            text = "Fallback Position",
            style = TextStyle(fontSize = 10.sp, color = Color.Yellow)
        )
        drawText(
            textLayoutResult = label,
            topLeft = Offset(dragRect.left, dragRect.bottom + 4f)
        )
    }
}
