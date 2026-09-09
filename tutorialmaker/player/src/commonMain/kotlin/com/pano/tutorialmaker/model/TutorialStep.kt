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
enum class AdvanceCondition {
    /** Advance as soon as the target is tapped (default). */
    TAP,
    /**
     * Advance only once the target's tag is no longer on screen — e.g. a dialog that only
     * closes when the user's input was actually accepted. If the tap doesn't make the target
     * go away (validation failed, dialog stayed open), the step just keeps waiting instead of
     * advancing on a click that didn't actually accomplish anything.
     */
    TARGET_DISMISSED
}

/**
 * An alternate path out of a WALKTHROUGH step: if the user taps [tag] instead of the step's
 * main target, jump straight to [nextStepId] (which must be a step in the same section) rather
 * than advancing to the next step in sequence.
 */
@Serializable
data class StepBranch(
    val tag: String = "",
    val nextStepId: String = ""
)

@Serializable
data class TutorialStep(
    val id: String,
    val target: TargetSpec = TargetSpec(),
    val spotlightShape: SpotlightShape = SpotlightShape.ROUNDED_RECT,
    val spotlightPaddingDp: Float = 8f,
    val text: String = "",
    /** Text shown in Help Mode's on-demand tooltip. Falls back to [text] when blank. */
    val infoText: String = "",
    val textPosition: TextPosition = TextPosition.BELOW,
    val textOffsetXDp: Float = 0f,
    val textOffsetYDp: Float = 0f,
    val mode: StepMode = StepMode.TOOLTIP,
    val dismissOnTargetClick: Boolean = true,
    val scrollTrigger: ScrollTrigger? = null,
    /** WALKTHROUGH only — when the tap on the main target actually advances the tutorial. */
    val advanceCondition: AdvanceCondition = AdvanceCondition.TAP,
    /** WALKTHROUGH only — alternate targets that jump elsewhere instead of advancing normally. */
    val branches: List<StepBranch> = emptyList()
)
