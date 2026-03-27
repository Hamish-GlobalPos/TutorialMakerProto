package com.pano.tutorialmaker.editor.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.pano.tutorialmaker.model.ScrollTrigger
import com.pano.tutorialmaker.model.SpotlightShape
import com.pano.tutorialmaker.model.StepMode
import com.pano.tutorialmaker.model.TextPosition
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.tagging.TutorialTagRegistry
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private enum class ScrollDragTarget {
    SPOTLIGHT_BODY,
    SPOTLIGHT_TOP_LEFT, SPOTLIGHT_TOP_RIGHT, SPOTLIGHT_BOTTOM_LEFT, SPOTLIGHT_BOTTOM_RIGHT,
    SPOTLIGHT_TOP, SPOTLIGHT_BOTTOM, SPOTLIGHT_LEFT, SPOTLIGHT_RIGHT,
    TRIGGER_Y, TRIGGER_X,
    NONE
}

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val viewportWidthPx = constraints.maxWidth.toFloat()
    val viewportHeightPx = constraints.maxHeight.toFloat()

    // In scroll mode the spotlight rect is draggable independently of the bound tag.
    // Stored as viewport fractions so position is preserved across window resizes.
    var scrollSpotlightRect by remember(step.id) {
        val t = step.target
        val xFrac = t.fallbackXFrac ?: 0.05f
        val yFrac = t.fallbackYFrac ?: 0.10f
        val wFrac = t.fallbackWidthFrac ?: 0.90f
        val hFrac = t.fallbackHeightFrac ?: 0.20f
        mutableStateOf(androidx.compose.ui.geometry.Rect(
            xFrac * viewportWidthPx,
            yFrac * viewportHeightPx,
            (xFrac + wFrac) * viewportWidthPx,
            (yFrac + hFrac) * viewportHeightPx
        ))
    }

    val isScroll = step.mode == StepMode.SCROLL
    val savedTrigger = step.scrollTrigger ?: if (isScroll) ScrollTrigger(yFraction = 0.5f) else null
    var triggerYFraction by remember(step.id, savedTrigger?.yFraction) { mutableStateOf(savedTrigger?.yFraction) }
    var triggerXFraction by remember(step.id, savedTrigger?.xFraction) { mutableStateOf(savedTrigger?.xFraction) }

    var scrollDragTarget by remember { mutableStateOf(ScrollDragTarget.NONE) }

    val dashedStroke = remember {
        Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
    }

    // Canvas for tag outlines + tap to bind
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(step.id, isScroll) {
                if (!isScroll) return@pointerInput
                val handleHitPx = with(density) { 20.dp.toPx() }
                val triggerHitPx = with(density) { 24.dp.toPx() }
                val minSize = 40f

                fun hitTest(offset: Offset): ScrollDragTarget {
                    val r = scrollSpotlightRect
                    // Corner handles (priority)
                    if ((offset - r.topLeft).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_TOP_LEFT
                    if ((offset - Offset(r.right, r.top)).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_TOP_RIGHT
                    if ((offset - Offset(r.left, r.bottom)).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_BOTTOM_LEFT
                    if ((offset - r.bottomRight).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_BOTTOM_RIGHT
                    // Edge handles
                    if ((offset - Offset(r.center.x, r.top)).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_TOP
                    if ((offset - Offset(r.center.x, r.bottom)).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_BOTTOM
                    if ((offset - Offset(r.left, r.center.y)).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_LEFT
                    if ((offset - Offset(r.right, r.center.y)).getDistance() < handleHitPx) return ScrollDragTarget.SPOTLIGHT_RIGHT
                    // Body
                    if (r.contains(offset)) return ScrollDragTarget.SPOTLIGHT_BODY
                    // Trigger lines
                    if (triggerYFraction?.let { abs(offset.y - it * size.height) < triggerHitPx } == true) return ScrollDragTarget.TRIGGER_Y
                    if (triggerXFraction?.let { abs(offset.x - it * size.width) < triggerHitPx } == true) return ScrollDragTarget.TRIGGER_X
                    return ScrollDragTarget.NONE
                }

                detectDragGestures(
                    onDragStart = { offset -> scrollDragTarget = hitTest(offset) },
                    onDragEnd = {
                        when (scrollDragTarget) {
                            ScrollDragTarget.TRIGGER_Y, ScrollDragTarget.TRIGGER_X ->
                                onStepChanged(step.copy(scrollTrigger = ScrollTrigger(triggerYFraction, triggerXFraction)))
                            ScrollDragTarget.NONE -> Unit
                            else -> onStepChanged(step.copy(target = step.target.copy(
                                fallbackXFrac = scrollSpotlightRect.left / viewportWidthPx,
                                fallbackYFrac = scrollSpotlightRect.top / viewportHeightPx,
                                fallbackWidthFrac = scrollSpotlightRect.width / viewportWidthPx,
                                fallbackHeightFrac = scrollSpotlightRect.height / viewportHeightPx
                            )))
                        }
                        scrollDragTarget = ScrollDragTarget.NONE
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val r = scrollSpotlightRect
                    scrollSpotlightRect = when (scrollDragTarget) {
                        ScrollDragTarget.SPOTLIGHT_BODY -> r.translate(dragAmount)
                        ScrollDragTarget.SPOTLIGHT_TOP_LEFT -> Rect((r.left + dragAmount.x).coerceAtMost(r.right - minSize), (r.top + dragAmount.y).coerceAtMost(r.bottom - minSize), r.right, r.bottom)
                        ScrollDragTarget.SPOTLIGHT_TOP_RIGHT -> Rect(r.left, (r.top + dragAmount.y).coerceAtMost(r.bottom - minSize), (r.right + dragAmount.x).coerceAtLeast(r.left + minSize), r.bottom)
                        ScrollDragTarget.SPOTLIGHT_BOTTOM_LEFT -> Rect((r.left + dragAmount.x).coerceAtMost(r.right - minSize), r.top, r.right, (r.bottom + dragAmount.y).coerceAtLeast(r.top + minSize))
                        ScrollDragTarget.SPOTLIGHT_BOTTOM_RIGHT -> Rect(r.left, r.top, (r.right + dragAmount.x).coerceAtLeast(r.left + minSize), (r.bottom + dragAmount.y).coerceAtLeast(r.top + minSize))
                        ScrollDragTarget.SPOTLIGHT_TOP -> Rect(r.left, (r.top + dragAmount.y).coerceAtMost(r.bottom - minSize), r.right, r.bottom)
                        ScrollDragTarget.SPOTLIGHT_BOTTOM -> Rect(r.left, r.top, r.right, (r.bottom + dragAmount.y).coerceAtLeast(r.top + minSize))
                        ScrollDragTarget.SPOTLIGHT_LEFT -> Rect((r.left + dragAmount.x).coerceAtMost(r.right - minSize), r.top, r.right, r.bottom)
                        ScrollDragTarget.SPOTLIGHT_RIGHT -> Rect(r.left, r.top, (r.right + dragAmount.x).coerceAtLeast(r.left + minSize), r.bottom)
                        ScrollDragTarget.TRIGGER_Y -> { triggerYFraction = triggerYFraction?.plus(dragAmount.y / size.height)?.coerceIn(0f, 1f); r }
                        ScrollDragTarget.TRIGGER_X -> { triggerXFraction = triggerXFraction?.plus(dragAmount.x / size.width)?.coerceIn(0f, 1f); r }
                        ScrollDragTarget.NONE -> r
                    }
                }
            }
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

        // In scroll mode: draw the independent spotlight box with shape + handles
        if (isScroll) {
            val spotColor = Color.Yellow.copy(alpha = 0.9f)
            val spotStroke = Stroke(width = 3f)
            val r = scrollSpotlightRect
            when (step.spotlightShape) {
                SpotlightShape.CIRCLE -> {
                    val radius = max(r.width, r.height) / 2f
                    drawCircle(spotColor, radius = radius, center = r.center, style = spotStroke)
                }
                SpotlightShape.RECT -> drawRect(spotColor, r.topLeft, r.size, style = spotStroke)
                SpotlightShape.ROUNDED_RECT -> drawRoundRect(spotColor, r.topLeft, r.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f), style = spotStroke)
            }
            // Corner handles
            listOf(r.topLeft, Offset(r.right, r.top), Offset(r.left, r.bottom), r.bottomRight)
                .forEach { drawCircle(spotColor, radius = 8f, center = it) }
            // Edge handles
            listOf(Offset(r.center.x, r.top), Offset(r.center.x, r.bottom),
                   Offset(r.left, r.center.y), Offset(r.right, r.center.y))
                .forEach { drawRect(spotColor, Offset(it.x - 6f, it.y - 6f), androidx.compose.ui.geometry.Size(12f, 12f)) }
            val spotLabel = textMeasurer.measure(
                "Spotlight · ${step.spotlightShape.name.lowercase().replace('_',' ')}",
                TextStyle(fontSize = 10.sp, color = spotColor)
            )
            drawText(spotLabel, topLeft = Offset(r.left, r.bottom + 4f))
        }

        // Draw scroll trigger lines
        if (isScroll) {
            val triggerColor = Color.Red.copy(alpha = 0.9f)
            val triggerDash = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
            val triggerStroke = with(density) { 3.dp.toPx() }
            triggerYFraction?.let { frac ->
                val y = frac * size.height
                drawLine(triggerColor, Offset(0f, y), Offset(size.width, y), triggerStroke, pathEffect = triggerDash)
                val label = textMeasurer.measure("Y ${(frac * 100).toInt()}%", TextStyle(fontSize = 10.sp, color = triggerColor))
                drawText(label, topLeft = Offset(8f, y - label.size.height - 4f))
            }
            triggerXFraction?.let { frac ->
                val x = frac * size.width
                drawLine(triggerColor, Offset(x, 0f), Offset(x, size.height), triggerStroke, pathEffect = triggerDash)
                val label = textMeasurer.measure("X ${(frac * 100).toInt()}%", TextStyle(fontSize = 10.sp, color = triggerColor))
                drawText(label, topLeft = Offset(x + 4f, 8f))
            }
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
    } // if (boundRect != null)
    } // BoxWithConstraints
}
