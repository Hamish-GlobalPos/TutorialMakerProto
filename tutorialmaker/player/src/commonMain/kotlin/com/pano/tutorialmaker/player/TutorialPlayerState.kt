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
        } else if (currentSectionIndex < tutorial.sections.size - 1) {
            // Cross section boundary
            currentSectionIndex++
            currentStepIndex = 0
        } else {
            // End of tutorial
            isActive = false
        }
    }

    fun previous() {
        if (currentStepIndex > 0) {
            currentStepIndex--
        } else if (currentSectionIndex > 0) {
            // Go back to previous section's last step
            currentSectionIndex--
            val prevSection = tutorial.sections[currentSectionIndex]
            currentStepIndex = (prevSection.steps.size - 1).coerceAtLeast(0)
        }
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
