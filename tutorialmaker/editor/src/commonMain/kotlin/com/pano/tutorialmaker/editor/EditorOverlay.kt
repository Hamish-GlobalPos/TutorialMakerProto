package com.pano.tutorialmaker.editor

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.editor.properties.StepPropertiesPanel
import com.pano.tutorialmaker.editor.timeline.SectionTimeline
import com.pano.tutorialmaker.editor.tools.EditorToolbar
import com.pano.tutorialmaker.editor.tools.HelpTextTool
import com.pano.tutorialmaker.editor.tools.SpotlightTool
import com.pano.tutorialmaker.editor.tools.TagOutlinesOverlay
import com.pano.tutorialmaker.io.TutorialFileManager
import com.pano.tutorialmaker.player.TutorialPlayer
import com.pano.tutorialmaker.tagging.TutorialTagRegistry
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorOverlay(
    fileManager: TutorialFileManager,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model = remember { EditorScreenModel(fileManager) }
    val state by model.state.collectAsState()
    val density = LocalDensity.current

    if (state.isPreviewMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            TutorialPlayer(
                tutorial = state.tutorial,
                onComplete = { model.togglePreviewMode() }
            )
        }
        return
    }

    var chromeVisible by remember { mutableStateOf(true) }
    var interactive by remember { mutableStateOf(false) }
    var fabOffset by remember { mutableStateOf(Offset(40f, 300f)) }
    var propertiesExpanded by remember { mutableStateOf(false) }

    // Tag outlines always visible — no touch interception
    TagOutlinesOverlay()

    Box(modifier = modifier.fillMaxSize()) {

        // Tool overlays — only when NOT in interactive mode
        if (!interactive) {
            val step = model.selectedStep
            if (step != null) {
                when (state.activeTool) {
                    EditorTool.SPOTLIGHT -> {
                        SpotlightTool(
                            stepId = step.id,
                            currentTarget = step.target,
                            onTargetChanged = { newTarget ->
                                model.updateStep(
                                    state.selectedSectionIndex,
                                    step.copy(target = newTarget)
                                )
                            }
                        )
                    }
                    EditorTool.HELP_TEXT -> {
                        val flatIndex = state.tutorial.sections
                            .take(state.selectedSectionIndex)
                            .sumOf { it.steps.size } + state.selectedStepIndex
                        val totalSteps = state.tutorial.sections.sumOf { it.steps.size }
                        val targetRect = TutorialTagRegistry.resolve(step.target, density)
                            ?: with(density) {
                                val x = (step.target.fallbackXDp ?: 100f).dp.toPx()
                                val y = (step.target.fallbackYDp ?: 100f).dp.toPx()
                                val w = (step.target.fallbackWidthDp ?: 120f).dp.toPx()
                                val h = (step.target.fallbackHeightDp ?: 48f).dp.toPx()
                                androidx.compose.ui.geometry.Rect(x, y, x + w, y + h)
                            }

                        HelpTextTool(
                            step = step,
                            targetRect = targetRect,
                            stepIndex = flatIndex,
                            totalSteps = totalSteps,
                            onStepChanged = { updatedStep ->
                                model.updateStep(state.selectedSectionIndex, updatedStep)
                            }
                        )
                    }
                }
            }
        }

        if (chromeVisible) {
            // Top bar
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Column {
                    OverlayTopBar(
                        tutorialName = state.tutorial.name,
                        availableTutorialIds = state.availableTutorialIds,
                        onClose = onClose,
                        onHideChrome = { chromeVisible = false },
                        isInteractive = interactive,
                        onToggleInteractive = { interactive = !interactive },
                        onTogglePreview = { model.togglePreviewMode() },
                        onSave = { model.saveTutorial() },
                        onLoad = { id -> model.loadTutorial(id) },
                        onNew = { model.newTutorial() },
                        onNameChanged = { model.updateTutorialName(it) }
                    )
                    EditorToolbar(
                        activeTool = state.activeTool,
                        onToolSelected = { model.setActiveTool(it) }
                    )
                }
            }

            // Bottom panel
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Column {
                    SectionTimeline(
                        sections = state.tutorial.sections,
                        selectedSectionIndex = state.selectedSectionIndex,
                        selectedStepIndex = state.selectedStepIndex,
                        onSelectSection = { model.selectSection(it) },
                        onSelectStep = { secIdx, stepIdx -> model.selectStep(secIdx, stepIdx) },
                        onAddSection = { model.addSection() },
                        onRemoveSection = { model.removeSection(it) },
                        onAddStep = { model.addStep(it) },
                        onRemoveStep = { secIdx, stepIdx -> model.removeStep(secIdx, stepIdx) },
                        onMoveStep = { secIdx, from, to -> model.reorderSteps(secIdx, from, to) }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { propertiesExpanded = !propertiesExpanded }) {
                            Icon(
                                if (propertiesExpanded) Icons.Default.KeyboardArrowDown
                                else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (propertiesExpanded) "Hide Properties" else "Show Properties"
                            )
                        }
                    }

                    if (propertiesExpanded) {
                        val currentStep = model.selectedStep
                        if (currentStep != null) {
                            StepPropertiesPanel(
                                step = currentStep,
                                onStepChanged = { updatedStep ->
                                    model.updateStep(state.selectedSectionIndex, updatedStep)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Add a section and step to begin editing",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Chrome hidden — draggable FAB to show it again
            Column(
                modifier = Modifier
                    .offset { IntOffset(fabOffset.x.roundToInt(), fabOffset.y.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            fabOffset = Offset(
                                fabOffset.x + dragAmount.x,
                                fabOffset.y + dragAmount.y
                            )
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Show chrome button
                SmallFloatingActionButton(
                    onClick = { chromeVisible = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("E", style = MaterialTheme.typography.titleSmall)
                }
                // Toggle interactive
                SmallFloatingActionButton(
                    onClick = { interactive = !interactive },
                    containerColor = if (interactive)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        if (interactive) "I" else "T",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayTopBar(
    tutorialName: String,
    availableTutorialIds: List<String>,
    onClose: () -> Unit,
    onHideChrome: () -> Unit,
    isInteractive: Boolean,
    onToggleInteractive: () -> Unit,
    onTogglePreview: () -> Unit,
    onSave: () -> Unit,
    onLoad: (String) -> Unit,
    onNew: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    var loadExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close Editor")
        }

        OutlinedTextField(
            value = tutorialName,
            onValueChange = onNameChanged,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )

        // Interactive toggle
        FilterChip(
            selected = isInteractive,
            onClick = onToggleInteractive,
            label = { Text(if (isInteractive) "Interactive" else "Editing") }
        )

        // Hide chrome
        IconButton(onClick = onHideChrome) {
            Text("H", style = MaterialTheme.typography.titleSmall)
        }

        IconButton(onClick = onNew) {
            Icon(Icons.Default.Add, contentDescription = "New Tutorial")
        }

        ExposedDropdownMenuBox(
            expanded = loadExpanded,
            onExpandedChange = { loadExpanded = it }
        ) {
            FilledTonalButton(
                onClick = { loadExpanded = true },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            ) {
                Text("Load")
            }
            ExposedDropdownMenu(
                expanded = loadExpanded,
                onDismissRequest = { loadExpanded = false }
            ) {
                if (availableTutorialIds.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No saved tutorials") },
                        onClick = { loadExpanded = false },
                        enabled = false
                    )
                } else {
                    availableTutorialIds.forEach { id ->
                        DropdownMenuItem(
                            text = { Text(id) },
                            onClick = {
                                onLoad(id)
                                loadExpanded = false
                            }
                        )
                    }
                }
            }
        }

        FilledTonalButton(onClick = onSave) {
            Text("Save")
        }

        IconButton(onClick = onTogglePreview) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Preview")
        }
    }
}
