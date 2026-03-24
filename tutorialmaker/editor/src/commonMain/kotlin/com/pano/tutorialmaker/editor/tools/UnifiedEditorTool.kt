package com.pano.tutorialmaker.editor.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pano.tutorialmaker.model.TextPosition
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.tagging.TutorialTagRegistry
import kotlin.math.roundToInt

@Composable
fun UnifiedEditorTool(
    step: TutorialStep,
    onStepChanged: (TutorialStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val boundTag = step.target.tag
    val boundRect = boundTag?.let { TutorialTagRegistry.elements[it] }

    var isEditingText by remember { mutableStateOf(false) }
    var dragOffsetX by remember(step.id) { mutableStateOf(step.textOffsetXDp) }
    var dragOffsetY by remember(step.id) { mutableStateOf(step.textOffsetYDp) }

    val dashedStroke = remember {
        Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
    }

    // Canvas for tag outlines + tap to bind
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(step.id, step.target) {
                detectTapGestures(
                    onTap = { offset ->
                        if (isEditingText) {
                            isEditingText = false
                            return@detectTapGestures
                        }
                        for ((tag, rect) in TutorialTagRegistry.elements) {
                            if (rect.contains(offset)) {
                                onStepChanged(step.copy(target = step.target.copy(tag = tag)))
                                return@detectTapGestures
                            }
                        }
                    }
                )
            }
    ) {
        // Draw all tag outlines
        TutorialTagRegistry.elements.forEach { (tag, rect) ->
            val isBound = tag == boundTag
            val outlineColor = if (isBound) Color.Green else Color.Cyan
            val alpha = if (isBound) 0.8f else 0.5f
            val stroke = if (isBound) Stroke(width = 3f) else dashedStroke

            drawRect(
                color = outlineColor.copy(alpha = alpha),
                topLeft = rect.topLeft,
                size = rect.size,
                style = stroke
            )
            val label = textMeasurer.measure(
                text = if (isBound) "$tag (bound)" else tag,
                style = TextStyle(fontSize = 10.sp, color = outlineColor.copy(alpha = alpha))
            )
            drawText(
                textLayoutResult = label,
                topLeft = Offset(rect.left, rect.top - label.size.height)
            )
        }

        // Draw guideline from target to text position
        if (boundRect != null) {
            val bubblePadding = with(density) { 16.dp.toPx() }
            val anchorPoint = with(density) {
                when (step.textPosition) {
                    TextPosition.ABOVE -> Offset(boundRect.left + dragOffsetX.dp.toPx(), boundRect.top - bubblePadding + dragOffsetY.dp.toPx())
                    TextPosition.BELOW -> Offset(boundRect.left + dragOffsetX.dp.toPx(), boundRect.bottom + bubblePadding + dragOffsetY.dp.toPx())
                    TextPosition.LEFT -> Offset(boundRect.left - bubblePadding + dragOffsetX.dp.toPx(), boundRect.center.y + dragOffsetY.dp.toPx())
                    TextPosition.RIGHT -> Offset(boundRect.right + bubblePadding + dragOffsetX.dp.toPx(), boundRect.center.y + dragOffsetY.dp.toPx())
                    TextPosition.CENTER -> Offset(boundRect.center.x + dragOffsetX.dp.toPx(), boundRect.center.y + dragOffsetY.dp.toPx())
                }
            }
            drawLine(
                color = Color.Magenta.copy(alpha = 0.5f),
                start = boundRect.center,
                end = anchorPoint,
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        }
    }

    // Text bubble — draggable + double-tap to edit
    if (boundRect != null) {
        val bubblePadding = 16.dp
        val offsetX: Float
        val offsetY: Float

        with(density) {
            when (step.textPosition) {
                TextPosition.ABOVE -> {
                    offsetX = boundRect.left + dragOffsetX.dp.toPx()
                    offsetY = boundRect.top - bubblePadding.toPx() + dragOffsetY.dp.toPx()
                }
                TextPosition.BELOW -> {
                    offsetX = boundRect.left + dragOffsetX.dp.toPx()
                    offsetY = boundRect.bottom + bubblePadding.toPx() + dragOffsetY.dp.toPx()
                }
                TextPosition.LEFT -> {
                    offsetX = boundRect.left - bubblePadding.toPx() + dragOffsetX.dp.toPx()
                    offsetY = boundRect.center.y + dragOffsetY.dp.toPx()
                }
                TextPosition.RIGHT -> {
                    offsetX = boundRect.right + bubblePadding.toPx() + dragOffsetX.dp.toPx()
                    offsetY = boundRect.center.y + dragOffsetY.dp.toPx()
                }
                TextPosition.CENTER -> {
                    offsetX = boundRect.center.x + dragOffsetX.dp.toPx()
                    offsetY = boundRect.center.y + dragOffsetY.dp.toPx()
                }
            }
        }

        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .widthIn(max = 280.dp)
                .pointerInput(step.id) {
                    detectDragGestures(
                        onDragEnd = {
                            onStepChanged(step.copy(textOffsetXDp = dragOffsetX, textOffsetYDp = dragOffsetY))
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        with(density) {
                            dragOffsetX += dragAmount.x.toDp().value
                            dragOffsetY += dragAmount.y.toDp().value
                        }
                    }
                }
                .pointerInput(step.id) {
                    detectTapGestures(
                        onDoubleTap = { isEditingText = true },
                        onTap = {
                            if (isEditingText) {
                                isEditingText = false
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isEditingText)
                    MaterialTheme.colorScheme.surfaceContainerHighest
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            if (isEditingText) {
                BasicTextField(
                    value = step.text,
                    onValueChange = { onStepChanged(step.copy(text = it)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(12.dp).widthIn(min = 120.dp)
                )
            } else {
                Text(
                    text = step.text.ifEmpty { "(double-tap to edit)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (step.text.isEmpty())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
