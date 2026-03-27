package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
enum class StepMode {
    /** Show text bubble with Next/Back/Skip buttons */
    TOOLTIP,
    /** User must tap the target to advance — tap passes through to the app */
    WALKTHROUGH,
    /** Show hint text; step advances automatically when the scroll trigger line is crossed */
    SCROLL
}

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
    val mode: StepMode = StepMode.TOOLTIP,
    val dismissOnTargetClick: Boolean = true,
    val scrollTrigger: ScrollTrigger? = null
)
