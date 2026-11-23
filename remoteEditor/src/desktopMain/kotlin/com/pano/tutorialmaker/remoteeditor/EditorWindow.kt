package com.pano.tutorialmaker.remoteeditor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.editor.EditorScreenModel
import com.pano.tutorialmaker.editor.OverlayTopBar
import com.pano.tutorialmaker.editor.properties.StepPropertiesPanel
import com.pano.tutorialmaker.editor.timeline.SectionTimeline
import com.pano.tutorialmaker.editor.tools.UnifiedEditorTool
import com.pano.tutorialmaker.editor.validation.validateTutorial
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

/**
 * The editor tools on the PC. They read [TutorialTagRegistry], so this window keeps its own
 * copy of the registry filled from the device's snapshots (scaled to the mirror), and the
 * existing tools work unchanged.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorWindowContent(session: RemoteSession) {
    val model = remember { EditorScreenModel(session.workspace) }
    val state by model.state.collectAsState()
    val issues = validateTutorial(state.tutorial)

    // After tutorials are pulled from the device, refresh the list and open one if we're blank.
    LaunchedEffect(session.tutorialsVersion) {
        if (session.tutorialsVersion == 0) return@LaunchedEffect
        model.refreshTutorialList()
        val s = model.state.value
        if (s.tutorial.sections.isEmpty() && !s.canUndo) {
            session.workspace.listTutorials().firstOrNull()?.let { model.loadTutorial(it) }
        }
    }

    fun saveAndPush() {
        val oldId = model.state.value.tutorial.id
        model.saveTutorial()
        val newId = model.state.value.tutorial.id
        // Ids derive from the name, so a rename moves the file: remove the stale one on the device too.
        if (newId != oldId) session.deleteTutorialOnDevice(oldId)
        session.pushTutorial(newId)
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.S -> { saveAndPush(); true }
                    Key.Z -> { if (event.isShiftPressed) model.redo() else model.undo(); true }
                    else -> false
                }
            }
    ) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 4.dp) {
            Column {
                OverlayTopBar(
                    tutorialName = state.tutorial.name,
                    availableTutorialIds = state.availableTutorialIds,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    issues = issues,
                    onTogglePreview = {
                        saveAndPush()
                        val tutorial = model.state.value.tutorial
                        val fromIndex = model.state.value.selectedSectionIndex
                        session.previewOnDevice(tutorial.id, tutorial.sections.drop(fromIndex).map { it.id })
                    },
                    onSave = ::saveAndPush,
                    onLoad = { model.loadTutorial(it) },
                    onDelete = { id ->
                        model.deleteTutorial(id)
                        session.deleteTutorialOnDevice(id)
                    },
                    onNew = { model.newTutorial() },
                    onNameChanged = { model.updateTutorialName(it) },
                    onUndo = { model.undo() },
                    onRedo = { model.redo() },
                    onJumpToIssue = { secIdx, stepIdx ->
                        model.selectSection(secIdx)
                        stepIdx?.let { model.selectStep(secIdx, it) }
                    }
                )
                Text(
                    if (session.connected) "Device connected  -  Save (Ctrl+S) pushes to the device; play button replays from the selected section"
                    else session.status,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth()) {
            MirrorView(session, Modifier.weight(1f).fillMaxSize().padding(8.dp)) { scale ->
                val snap = session.snapshot
                // Registry coordinates = this mirror's local pixels, which is what the tools draw in.
                LaunchedEffect(snap, scale) {
                    Snapshot.withMutableSnapshot {
                        TutorialTagRegistry.elements.clear()
                        TutorialTagRegistry.screens.clear()
                        snap?.tags?.forEach {
                            TutorialTagRegistry.elements[it.tag] =
                                Rect(it.left * scale, it.top * scale, it.right * scale, it.bottom * scale)
                        }
                        snap?.screens?.forEach { TutorialTagRegistry.screens[it] = true }
                    }
                }
                val step = model.selectedStep
                if (step != null) {
                    UnifiedEditorTool(
                        step = step,
                        onStepChanged = { updated ->
                            model.updateStep(state.selectedSectionIndex, updated)
                            if (updated.target.tag != step.target.tag) {
                                model.autoPopulateSectionTags(state.selectedSectionIndex, updated)
                            }
                        }
                    )
                }
            }

            // StepPropertiesPanel scrolls itself, so it must get bounded height (no outer scroll).
            Box(Modifier.width(420.dp).fillMaxSize()) {
                val step = model.selectedStep
                if (step != null) {
                    StepPropertiesPanel(
                        step = step,
                        onStepChanged = { model.updateStep(state.selectedSectionIndex, it) },
                        sectionSteps = state.tutorial.sections.getOrNull(state.selectedSectionIndex)?.steps ?: emptyList(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Add a section and step to begin editing",
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                }
            }
        }

        // The timeline can be taller/wider than the window: it scrolls vertically (scrollbar at the
        // right), and the mouse wheel scrolls whichever row it is over sideways (section chips or
        // step cards).
        val timelineScroll = rememberScrollState()
        val sectionsScroll = rememberScrollState()
        val stepsScroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Column(Modifier.verticalScroll(timelineScroll)) {
                    SectionTimeline(
                        sections = state.tutorial.sections,
                        selectedSectionIndex = state.selectedSectionIndex,
                        selectedStepIndex = state.selectedStepIndex,
                        onSelectSection = { model.selectSection(it) },
                        onSelectStep = { secIdx, stepIdx -> model.selectStep(secIdx, stepIdx) },
                        onAddSection = { model.addSection() },
                        onRemoveSection = { model.removeSection(it) },
                        onMoveSection = { from, to -> model.reorderSections(from, to) },
                        onDuplicateSection = { model.duplicateSection(it) },
                        onAddStep = { model.addStep(it) },
                        onInsertStep = { secIdx, atIdx -> model.insertStep(secIdx, atIdx) },
                        onRemoveStep = { secIdx, stepIdx -> model.removeStep(secIdx, stepIdx) },
                        onDuplicateStep = { secIdx, stepIdx -> model.duplicateStep(secIdx, stepIdx) },
                        onMoveStep = { secIdx, from, to -> model.reorderSteps(secIdx, from, to) },
                        onSectionChanged = { secIdx, section -> model.updateSection(secIdx, section) },
                        sectionsScrollState = sectionsScroll,
                        stepsScrollState = stepsScroll,
                        sectionsRowModifier = Modifier.wheelScrolls(sectionsScroll, scope),
                        stepsRowModifier = Modifier.wheelScrolls(stepsScroll, scope)
                    )
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(timelineScroll),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}

private const val WHEEL_STEP_PX = 120f

/** Mouse wheel over this element scrolls [state] sideways (desktop wheels only scroll vertical areas by default). */
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.wheelScrolls(state: ScrollState, scope: CoroutineScope): Modifier =
    onPointerEvent(PointerEventType.Scroll) { event ->
        val change = event.changes.firstOrNull() ?: return@onPointerEvent
        val delta = (change.scrollDelta.y + change.scrollDelta.x) * WHEEL_STEP_PX
        scope.launch { state.scrollBy(delta) }
        event.changes.forEach { it.consume() }
    }
