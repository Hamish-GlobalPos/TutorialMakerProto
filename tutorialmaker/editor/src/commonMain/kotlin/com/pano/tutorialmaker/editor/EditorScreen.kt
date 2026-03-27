package com.pano.tutorialmaker.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.pano.tutorialmaker.editor.properties.StepPropertiesPanel
import com.pano.tutorialmaker.editor.timeline.SectionTimeline
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import com.pano.tutorialmaker.editor.tools.EditorToolbar
import com.pano.tutorialmaker.editor.tools.HelpTextTool
import com.pano.tutorialmaker.editor.tools.ScrollTriggerTool
import com.pano.tutorialmaker.editor.tools.SpotlightTool
import com.pano.tutorialmaker.io.TutorialFileManager
import com.pano.tutorialmaker.player.SpotlightOverlay
import com.pano.tutorialmaker.player.TutorialPlayer
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

class EditorScreen(
    private val fileManager: TutorialFileManager
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = remember { EditorScreenModel(fileManager) }
        val state by model.state.collectAsState()
        val density = LocalDensity.current

        if (state.isPreviewMode) {
            // Preview mode: run the tutorial player
            Box(modifier = Modifier.fillMaxSize()) {
                TutorialPlayer(
                    tutorial = state.tutorial,
                    onComplete = { model.togglePreviewMode() }
                )
            }
            return
        }

        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
        )

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 160.dp,
            topBar = {
                EditorTopBar(
                    tutorialName = state.tutorial.name,
                    isPreviewMode = state.isPreviewMode,
                    availableTutorialIds = state.availableTutorialIds,
                    onBack = { navigator.pop() },
                    onToggleMode = { model.togglePreviewMode() },
                    onSave = { model.saveTutorial() },
                    onLoad = { id -> model.loadTutorial(id) },
                    onNew = { model.newTutorial() },
                    onNameChanged = { model.updateTutorialName(it) }
                )
            },
            sheetContent = {
                // Properties panel in bottom sheet
                val step = model.selectedStep
                if (step != null) {
                    StepPropertiesPanel(
                        step = step,
                        onStepChanged = { updatedStep ->
                            model.updateStep(state.selectedSectionIndex, updatedStep)
                        }
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
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Toolbar for tool selection
                EditorToolbar(
                    activeTool = state.activeTool,
                    onToolSelected = { model.setActiveTool(it) }
                )

                // Preview area with tool overlays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val step = model.selectedStep
                    if (step != null) {
                        val resolvedRect = TutorialTagRegistry.resolve(step.target, density)
                        if (resolvedRect != null) {
                            SpotlightOverlay(
                                targetRect = resolvedRect,
                                shape = step.spotlightShape,
                                paddingPx = with(density) { step.spotlightPaddingDp.dp.toPx() },
                                scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)
                            )
                        }

                        // Always show trigger lines when in Scroll mode
                        val triggerForDisplay = step.scrollTrigger
                            ?: if (step.mode == com.pano.tutorialmaker.model.StepMode.SCROLL)
                                com.pano.tutorialmaker.model.ScrollTrigger(yFraction = 0.5f) else null
                        triggerForDisplay?.let { trigger ->
                            if (state.activeTool != EditorTool.SCROLL_TRIGGER) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val lineColor = Color.Red.copy(alpha = 0.9f)
                                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                                    trigger.yFraction?.let { frac ->
                                        val y = frac * size.height
                                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y),
                                            strokeWidth = 4f, pathEffect = dashEffect)
                                    }
                                    trigger.xFraction?.let { frac ->
                                        val x = frac * size.width
                                        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height),
                                            strokeWidth = 4f, pathEffect = dashEffect)
                                    }
                                }
                            }
                        }

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
                                        androidx.compose.ui.geometry.Rect(100f.dp.toPx(), 100f.dp.toPx(), 220f.dp.toPx(), 148f.dp.toPx())
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
                            EditorTool.SCROLL_TRIGGER -> {
                                ScrollTriggerTool(
                                    step = step,
                                    onStepChanged = { updatedStep ->
                                        model.updateStep(state.selectedSectionIndex, updatedStep)
                                    }
                                )
                            }
                        }
                    }
                }

                // Timeline at the bottom
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
                    onMoveStep = { secIdx, from, to -> model.reorderSteps(secIdx, from, to) },
                    onSectionChanged = { secIdx, section -> model.updateSection(secIdx, section) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    tutorialName: String,
    isPreviewMode: Boolean,
    availableTutorialIds: List<String>,
    onBack: () -> Unit,
    onToggleMode: () -> Unit,
    onSave: () -> Unit,
    onLoad: (String) -> Unit,
    onNew: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    var loadExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            OutlinedTextField(
                value = tutorialName,
                onValueChange = onNameChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // New
            IconButton(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = "New Tutorial")
            }

            // Load dropdown
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

            // Save
            FilledTonalButton(onClick = onSave) {
                Text("Save")
            }

            // Mode toggle
            IconButton(onClick = onToggleMode) {
                Icon(
                    if (isPreviewMode) Icons.Default.Edit else Icons.Default.PlayArrow,
                    contentDescription = if (isPreviewMode) "Edit Mode" else "Preview Mode"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}
