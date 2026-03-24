package com.pano.tutorialmaker.editor

import cafe.adriel.voyager.core.model.ScreenModel
import com.pano.tutorialmaker.io.TutorialFileManager
import com.pano.tutorialmaker.model.TriggerType
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.model.TutorialSection
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.tagging.TutorialTagRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EditorState(
    val tutorial: Tutorial = Tutorial(id = "new_tutorial", name = "New Tutorial"),
    val selectedSectionIndex: Int = 0,
    val selectedStepIndex: Int = 0,
    val isPreviewMode: Boolean = false,
    val activeTool: EditorTool = EditorTool.SPOTLIGHT,
    val availableTutorialIds: List<String> = emptyList()
)

class EditorScreenModel(
    private val fileManager: TutorialFileManager
) : ScreenModel {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        refreshTutorialList()
        // Auto-load the most recent tutorial
        val ids = fileManager.listTutorials()
        if (ids.isNotEmpty()) {
            loadTutorial(ids.first())
        }
    }

    val selectedStep: TutorialStep?
        get() {
            val s = _state.value
            return s.tutorial.sections
                .getOrNull(s.selectedSectionIndex)
                ?.steps?.getOrNull(s.selectedStepIndex)
        }

    // --- Selection ---

    fun selectSection(index: Int) {
        _state.update { it.copy(selectedSectionIndex = index, selectedStepIndex = 0) }
    }

    fun selectStep(sectionIndex: Int, stepIndex: Int) {
        _state.update { it.copy(selectedSectionIndex = sectionIndex, selectedStepIndex = stepIndex) }
    }

    // --- Sections ---

    fun addSection() {
        _state.update { state ->
            val sections = state.tutorial.sections
            val newId = "section_${sections.size + 1}"
            val newSection = TutorialSection(
                id = newId,
                title = "Section ${sections.size + 1}",
                steps = listOf(TutorialStep(id = "${newId}_step_1", text = "New step"))
            )
            state.copy(
                tutorial = state.tutorial.copy(sections = sections + newSection),
                selectedSectionIndex = sections.size,
                selectedStepIndex = 0
            )
        }
    }

    fun autoPopulateSectionTags(sectionIndex: Int, step: TutorialStep) {
        val tag = step.target.tag ?: return
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            val section = sections.getOrNull(sectionIndex) ?: return@update state

            // Find which screen this tag is on (use the first active screen)
            val activeScreenTag = TutorialTagRegistry.screens.keys.firstOrNull()

            val updated = when (section.triggerType) {
                TriggerType.ELEMENT -> section.copy(
                    screenTag = activeScreenTag ?: section.screenTag,
                    elementTag = tag
                )
                else -> section.copy(
                    screenTag = activeScreenTag ?: section.screenTag
                )
            }

            sections[sectionIndex] = updated
            state.copy(tutorial = state.tutorial.copy(sections = sections))
        }
    }

    fun updateSection(index: Int, section: TutorialSection) {
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            if (index !in sections.indices) return@update state
            sections[index] = section
            state.copy(tutorial = state.tutorial.copy(sections = sections))
        }
    }

    fun removeSection(index: Int) {
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            if (index !in sections.indices) return@update state
            sections.removeAt(index)
            state.copy(
                tutorial = state.tutorial.copy(sections = sections),
                selectedSectionIndex = (state.selectedSectionIndex).coerceAtMost((sections.size - 1).coerceAtLeast(0)),
                selectedStepIndex = 0
            )
        }
    }

    fun reorderSections(fromIndex: Int, toIndex: Int) {
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            if (fromIndex !in sections.indices || toIndex !in sections.indices) return@update state
            val item = sections.removeAt(fromIndex)
            sections.add(toIndex, item)
            state.copy(
                tutorial = state.tutorial.copy(sections = sections),
                selectedSectionIndex = toIndex
            )
        }
    }

    // --- Steps ---

    fun addStep(sectionIndex: Int) {
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            val section = sections.getOrNull(sectionIndex) ?: return@update state
            val steps = section.steps.toMutableList()
            val newStep = TutorialStep(
                id = "${section.id}_step_${steps.size + 1}",
                text = "New step"
            )
            steps.add(newStep)
            sections[sectionIndex] = section.copy(steps = steps)
            state.copy(
                tutorial = state.tutorial.copy(sections = sections),
                selectedSectionIndex = sectionIndex,
                selectedStepIndex = steps.size - 1
            )
        }
    }

    fun removeStep(sectionIndex: Int, stepIndex: Int) {
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            val section = sections.getOrNull(sectionIndex) ?: return@update state
            val steps = section.steps.toMutableList()
            if (stepIndex !in steps.indices) return@update state
            steps.removeAt(stepIndex)
            sections[sectionIndex] = section.copy(steps = steps)
            state.copy(
                tutorial = state.tutorial.copy(sections = sections),
                selectedStepIndex = (state.selectedStepIndex).coerceAtMost((steps.size - 1).coerceAtLeast(0))
            )
        }
    }

    fun reorderSteps(sectionIndex: Int, fromIndex: Int, toIndex: Int) {
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            val section = sections.getOrNull(sectionIndex) ?: return@update state
            val steps = section.steps.toMutableList()
            if (fromIndex !in steps.indices || toIndex !in steps.indices) return@update state
            val item = steps.removeAt(fromIndex)
            steps.add(toIndex, item)
            sections[sectionIndex] = section.copy(steps = steps)
            state.copy(
                tutorial = state.tutorial.copy(sections = sections),
                selectedStepIndex = toIndex
            )
        }
    }

    fun updateStep(sectionIndex: Int, step: TutorialStep) {
        _state.update { state ->
            val sections = state.tutorial.sections.toMutableList()
            val section = sections.getOrNull(sectionIndex) ?: return@update state
            val steps = section.steps.toMutableList()
            val idx = steps.indexOfFirst { it.id == step.id }
            if (idx == -1) return@update state
            steps[idx] = step
            sections[sectionIndex] = section.copy(steps = steps)
            state.copy(tutorial = state.tutorial.copy(sections = sections))
        }
    }

    // --- Tutorial metadata ---

    fun updateTutorialName(name: String) {
        _state.update { it.copy(tutorial = it.tutorial.copy(name = name)) }
    }

    fun updateTutorialDescription(description: String) {
        _state.update { it.copy(tutorial = it.tutorial.copy(description = description)) }
    }

    // --- Mode ---

    fun setActiveTool(tool: EditorTool) {
        _state.update { it.copy(activeTool = tool) }
    }

    fun togglePreviewMode() {
        _state.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    // --- File I/O ---

    fun saveTutorial() {
        val tutorial = _state.value.tutorial
        // Use the name as the ID so saves are name-based
        val idFromName = tutorial.name.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "untitled" }
        val updated = tutorial.copy(id = idFromName)
        _state.update { it.copy(tutorial = updated) }
        fileManager.saveTutorial(updated)
        refreshTutorialList()
    }

    fun loadTutorial(id: String) {
        val tutorial = fileManager.loadTutorial(id) ?: return
        _state.update {
            it.copy(
                tutorial = tutorial,
                selectedSectionIndex = 0,
                selectedStepIndex = 0,
                isPreviewMode = false
            )
        }
    }

    fun newTutorial() {
        val id = "tutorial_${System.currentTimeMillis()}"
        _state.update {
            EditorState(
                tutorial = Tutorial(id = id, name = "New Tutorial"),
                availableTutorialIds = it.availableTutorialIds
            )
        }
    }

    fun refreshTutorialList() {
        val ids = fileManager.listTutorials()
        _state.update { it.copy(availableTutorialIds = ids) }
    }
}
