package com.pano.tutorialmaker.editor.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

@Composable
fun TagOutlinesOverlay(
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val dashedStroke = remember {
        Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        TutorialTagRegistry.elements.forEach { (tag, rect) ->
            drawRect(
                color = Color.Cyan.copy(alpha = 0.5f),
                topLeft = rect.topLeft,
                size = rect.size,
                style = dashedStroke
            )
            val label = textMeasurer.measure(
                text = tag,
                style = TextStyle(fontSize = 10.sp, color = Color.Cyan)
            )
            drawText(
                textLayoutResult = label,
                topLeft = Offset(rect.left, rect.top - label.size.height)
            )
        }
    }
}
