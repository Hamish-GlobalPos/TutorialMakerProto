package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
data class TutorialStep(
    val id: String,
    val target: TargetSpec = TargetSpec(),
    val spotlightShape: SpotlightShape = SpotlightShape.ROUNDED_RECT,
    val spotlightPaddingDp: Float = 8f,
    val text: String = "",
    val textPosition: TextPosition = TextPosition.BELOW,
    val textOffsetXDp: Float = 0f,
    val textOffsetYDp: Float = 0f,
    val dismissOnTargetClick: Boolean = false
)
