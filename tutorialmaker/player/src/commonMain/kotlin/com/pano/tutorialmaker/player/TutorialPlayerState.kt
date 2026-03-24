package com.pano.tutorialmaker.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.model.TutorialStep

class TutorialPlayerState(
    val tutorial: Tutorial
) {
    var currentSectionIndex by mutableIntStateOf(0)
        private set
    var currentStepIndex by mutableIntStateOf(0)
        private set
    var isActive by mutableStateOf(true)
        private set

    private val flatSteps: List<TutorialStep> = tutorial.sections.flatMap { it.steps }

    val totalSteps: Int get() = flatSteps.size

    /** Steps in the current section */
    val currentSectionStepCount: Int
        get() = tutorial.sections.getOrNull(currentSectionIndex)?.steps?.size ?: 0

    val flatStepIndex: Int
        get() {
            var index = 0
            for (s in 0 until currentSectionIndex) {
                index += tutorial.sections[s].steps.size
            }
            return index + currentStepIndex
        }

    val currentStep: TutorialStep?
        get() {
            val section = tutorial.sections.getOrNull(currentSectionIndex) ?: return null
            return section.steps.getOrNull(currentStepIndex)
        }

    fun next() {
        val section = tutorial.sections.getOrNull(currentSectionIndex) ?: return

        if (currentStepIndex < section.steps.size - 1) {
            currentStepIndex++
        } else {
            // End of section — stop. SectionTrigger handles queuing next sections.
            isActive = false
        }
    }

    fun previous() {
        if (currentStepIndex > 0) {
            currentStepIndex--
        }
        // Don't go back across section boundaries
    }

    fun skip() {
        isActive = false
    }

    fun restart() {
        currentSectionIndex = 0
        currentStepIndex = 0
        isActive = true
    }
}

@Composable
fun rememberTutorialPlayerState(tutorial: Tutorial): TutorialPlayerState {
    return remember(tutorial) { TutorialPlayerState(tutorial) }
}
