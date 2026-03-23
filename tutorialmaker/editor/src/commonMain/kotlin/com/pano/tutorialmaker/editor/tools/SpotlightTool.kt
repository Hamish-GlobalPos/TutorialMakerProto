package com.pano.tutorialmaker.editor.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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

private const val HANDLE_RADIUS = 8f

private enum class DragHandle {
    BODY,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT
}

@Composable
fun SpotlightTool(
    stepId: String,
    currentTarget: TargetSpec,
    onTargetChanged: (TargetSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // The bound tag (if any) — when bound, the yellow rect follows the tag's rect
    val boundTag = currentTarget.tag
    val boundRect = boundTag?.let { TutorialTagRegistry.elements[it] }

    var dragRect by remember(stepId, currentTarget) {
        with(density) {
            if (boundRect != null) {
                mutableStateOf(boundRect)
            } else {
                val x = (currentTarget.fallbackXDp ?: 100f).dp.toPx()
                val y = (currentTarget.fallbackYDp ?: 100f).dp.toPx()
                val w = (currentTarget.fallbackWidthDp ?: 120f).dp.toPx()
                val h = (currentTarget.fallbackHeightDp ?: 48f).dp.toPx()
                mutableStateOf(Rect(x, y, x + w, y + h))
            }
        }
    }

    // Keep dragRect in sync with bound tag
    if (boundRect != null) {
        dragRect = boundRect
    }

    var activeHandle by remember { mutableStateOf<DragHandle?>(null) }

    fun commitRect() {
        with(density) {
            onTargetChanged(
                currentTarget.copy(
                    fallbackXDp = dragRect.left.toDp().value,
                    fallbackYDp = dragRect.top.toDp().value,
                    fallbackWidthDp = dragRect.width.toDp().value,
                    fallbackHeightDp = dragRect.height.toDp().value
                )
            )
        }
    }

    fun hitTestHandle(pos: Offset): DragHandle {
        val r = HANDLE_RADIUS * 2.5f

        if ((pos - dragRect.topLeft).getDistance() < r) return DragHandle.TOP_LEFT
        if ((pos - Offset(dragRect.right, dragRect.top)).getDistance() < r) return DragHandle.TOP_RIGHT
        if ((pos - Offset(dragRect.left, dragRect.bottom)).getDistance() < r) return DragHandle.BOTTOM_LEFT
        if ((pos - dragRect.bottomRight).getDistance() < r) return DragHandle.BOTTOM_RIGHT

        val topMid = Offset(dragRect.center.x, dragRect.top)
        val bottomMid = Offset(dragRect.center.x, dragRect.bottom)
        val leftMid = Offset(dragRect.left, dragRect.center.y)
        val rightMid = Offset(dragRect.right, dragRect.center.y)

        if ((pos - topMid).getDistance() < r) return DragHandle.TOP
        if ((pos - bottomMid).getDistance() < r) return DragHandle.BOTTOM
        if ((pos - leftMid).getDistance() < r) return DragHandle.LEFT
        if ((pos - rightMid).getDistance() < r) return DragHandle.RIGHT

        return DragHandle.BODY
    }

    fun hitTestTag(pos: Offset): String? {
        for ((tag, rect) in TutorialTagRegistry.elements) {
            if (rect.contains(pos)) {
                return tag
            }
        }
        return null
    }

    val dashedStroke = remember {
        Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(stepId, currentTarget) {
                detectTapGestures { offset ->
                    val tappedTag = hitTestTag(offset)
                    if (tappedTag != null) {
                        // Bind spotlight to this tag
                        onTargetChanged(currentTarget.copy(tag = tappedTag))
                    }
                }
            }
            .pointerInput(stepId, currentTarget) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle = hitTestHandle(offset)
                    },
                    onDragEnd = {
                        commitRect()
                        activeHandle = null
                    },
                    onDragCancel = { activeHandle = null }
                ) { change, dragAmount ->
                    change.consume()
                    // If bound to a tag, don't allow dragging the rect
                    if (boundTag != null) return@detectDragGestures

                    val minSize = 20f
                    dragRect = when (activeHandle) {
                        DragHandle.BODY -> dragRect.translate(dragAmount)
                        DragHandle.TOP_LEFT -> Rect(
                            left = (dragRect.left + dragAmount.x).coerceAtMost(dragRect.right - minSize),
                            top = (dragRect.top + dragAmount.y).coerceAtMost(dragRect.bottom - minSize),
                            right = dragRect.right,
                            bottom = dragRect.bottom
                        )
                        DragHandle.TOP_RIGHT -> Rect(
                            left = dragRect.left,
                            top = (dragRect.top + dragAmount.y).coerceAtMost(dragRect.bottom - minSize),
                            right = (dragRect.right + dragAmount.x).coerceAtLeast(dragRect.left + minSize),
                            bottom = dragRect.bottom
                        )
                        DragHandle.BOTTOM_LEFT -> Rect(
                            left = (dragRect.left + dragAmount.x).coerceAtMost(dragRect.right - minSize),
                            top = dragRect.top,
                            right = dragRect.right,
                            bottom = (dragRect.bottom + dragAmount.y).coerceAtLeast(dragRect.top + minSize)
                        )
                        DragHandle.BOTTOM_RIGHT -> Rect(
                            left = dragRect.left,
                            top = dragRect.top,
                            right = (dragRect.right + dragAmount.x).coerceAtLeast(dragRect.left + minSize),
                            bottom = (dragRect.bottom + dragAmount.y).coerceAtLeast(dragRect.top + minSize)
                        )
                        DragHandle.TOP -> Rect(
                            left = dragRect.left,
                            top = (dragRect.top + dragAmount.y).coerceAtMost(dragRect.bottom - minSize),
                            right = dragRect.right,
                            bottom = dragRect.bottom
                        )
                        DragHandle.BOTTOM -> Rect(
                            left = dragRect.left,
                            top = dragRect.top,
                            right = dragRect.right,
                            bottom = (dragRect.bottom + dragAmount.y).coerceAtLeast(dragRect.top + minSize)
                        )
                        DragHandle.LEFT -> Rect(
                            left = (dragRect.left + dragAmount.x).coerceAtMost(dragRect.right - minSize),
                            top = dragRect.top,
                            right = dragRect.right,
                            bottom = dragRect.bottom
                        )
                        DragHandle.RIGHT -> Rect(
                            left = dragRect.left,
                            top = dragRect.top,
                            right = (dragRect.right + dragAmount.x).coerceAtLeast(dragRect.left + minSize),
                            bottom = dragRect.bottom
                        )
                        null -> dragRect
                    }
                }
            }
    ) {
        // Draw outlines of all registered tags — highlight the bound one
        TutorialTagRegistry.elements.forEach { (tag, rect) ->
            val isBound = tag == boundTag
            val outlineColor = if (isBound) Color.Green else Color.Cyan
            val outlineAlpha = if (isBound) 0.8f else 0.5f
            val strokeWidth = if (isBound) 3f else 2f

            drawRect(
                color = outlineColor.copy(alpha = outlineAlpha),
                topLeft = rect.topLeft,
                size = rect.size,
                style = if (isBound) Stroke(width = strokeWidth) else dashedStroke
            )
            val label = textMeasurer.measure(
                text = if (isBound) "$tag (bound)" else tag,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = outlineColor.copy(alpha = outlineAlpha)
                )
            )
            drawText(
                textLayoutResult = label,
                topLeft = Offset(rect.left, rect.top - label.size.height)
            )
        }

        // Draw the yellow rect (fallback position or follows bound tag)
        drawRect(
            color = if (boundTag != null) Color.Green.copy(alpha = 0.7f) else Color.Yellow.copy(alpha = 0.7f),
            topLeft = dragRect.topLeft,
            size = dragRect.size,
            style = Stroke(width = 3f)
        )

        // Only show handles if not bound to a tag (manual positioning mode)
        if (boundTag == null) {
            // Corner handles
            listOf(
                dragRect.topLeft,
                Offset(dragRect.right, dragRect.top),
                Offset(dragRect.left, dragRect.bottom),
                dragRect.bottomRight
            ).forEach { corner ->
                drawCircle(color = Color.Yellow, radius = HANDLE_RADIUS, center = corner)
            }

            // Edge midpoint handles
            listOf(
                Offset(dragRect.center.x, dragRect.top),
                Offset(dragRect.center.x, dragRect.bottom),
                Offset(dragRect.left, dragRect.center.y),
                Offset(dragRect.right, dragRect.center.y)
            ).forEach { mid ->
                drawSquareHandle(mid, HANDLE_RADIUS * 0.8f, Color.Yellow.copy(alpha = 0.8f))
            }
        }

        // Label
        val labelColor = if (boundTag != null) Color.Green else Color.Yellow
        val label = textMeasurer.measure(
            text = if (boundTag != null) "Bound to: $boundTag" else "Fallback Position (tap a component to bind)",
            style = TextStyle(fontSize = 10.sp, color = labelColor)
        )
        drawText(
            textLayoutResult = label,
            topLeft = Offset(dragRect.left, dragRect.bottom + 4f)
        )

        // Size label
        with(density) {
            val sizeLabel = textMeasurer.measure(
                text = "${dragRect.width.toDp().value.toInt()} x ${dragRect.height.toDp().value.toInt()} dp",
                style = TextStyle(fontSize = 9.sp, color = labelColor.copy(alpha = 0.7f))
            )
            drawText(
                textLayoutResult = sizeLabel,
                topLeft = Offset(dragRect.left, dragRect.bottom + 4f + label.size.height)
            )
        }
    }
}

private fun DrawScope.drawSquareHandle(center: Offset, halfSize: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(center.x - halfSize, center.y - halfSize),
        size = Size(halfSize * 2, halfSize * 2)
    )
}
