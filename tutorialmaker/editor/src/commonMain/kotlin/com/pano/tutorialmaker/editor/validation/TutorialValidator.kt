package com.pano.tutorialmaker.editor.validation

import com.pano.tutorialmaker.model.StepMode
import com.pano.tutorialmaker.model.TriggerType
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

/**
 * One thing worth the author's attention in a [Tutorial]. [sectionIndex]/[stepIndex] let the UI
 * jump straight to the offending section/step; [stepIndex] is null for section-level issues.
 */
data class ValidationIssue(
    val sectionIndex: Int,
    val stepIndex: Int?,
    val message: String
)

/**
 * Structural checks that don't depend on what's currently on screen, plus one best-effort
 * liveness check (a step's tag not being in [TutorialTagRegistry] right now) — that one can be a
 * false positive if the author simply hasn't navigated to that screen yet, so it's worded as a
 * possibility rather than a certainty.
 */
fun validateTutorial(tutorial: Tutorial): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    val seenSectionIds = mutableSetOf<String>()
    val liveTags = TutorialTagRegistry.elements.keys

    for ((sectionIndex, section) in tutorial.sections.withIndex()) {
        if (!seenSectionIds.add(section.id)) {
            issues += ValidationIssue(
                sectionIndex, null,
                "Duplicate section id \"${section.id}\" — progress tracking may collide with another section."
            )
        }

        if (section.screenTag.isNullOrBlank()) {
            issues += ValidationIssue(sectionIndex, null, "No screen assigned — this section will never trigger.")
        }
        if (section.triggerType == TriggerType.ELEMENT && section.elementTag.isNullOrBlank()) {
            issues += ValidationIssue(sectionIndex, null, "Element trigger has no target tag set.")
        }
        if (section.steps.isEmpty()) {
            issues += ValidationIssue(sectionIndex, null, "Section has no steps.")
        }

        val seenStepIds = mutableSetOf<String>()
        for ((stepIndex, step) in section.steps.withIndex()) {
            if (!seenStepIds.add(step.id)) {
                issues += ValidationIssue(sectionIndex, stepIndex, "Duplicate step id \"${step.id}\" in this section.")
            }
            if (step.text.isBlank()) {
                issues += ValidationIssue(sectionIndex, stepIndex, "Step has no text.")
            }

            val target = step.target
            val hasFallbackRect = target.fallbackXFrac != null && target.fallbackYFrac != null &&
                target.fallbackWidthFrac != null && target.fallbackHeightFrac != null

            if (target.tag.isNullOrBlank() && !hasFallbackRect) {
                issues += ValidationIssue(sectionIndex, stepIndex, "No target bound — step won't know where to point.")
            } else if (!target.tag.isNullOrBlank() && liveTags.isNotEmpty() && target.tag !in liveTags) {
                issues += ValidationIssue(
                    sectionIndex, stepIndex,
                    "Tag \"${target.tag}\" isn't currently visible — may just be on another screen, or may have been renamed/removed."
                )
            }

            if (step.mode == StepMode.SCROLL) {
                val trigger = step.scrollTrigger
                if (trigger == null || (trigger.yFraction == null && trigger.xFraction == null)) {
                    issues += ValidationIssue(sectionIndex, stepIndex, "Scroll step has no trigger line set.")
                }
            }
        }
    }

    return issues
}
