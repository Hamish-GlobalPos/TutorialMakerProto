package com.pano.tutorialmaker.editor.properties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.AdvanceCondition
import com.pano.tutorialmaker.model.ScrollTrigger
import com.pano.tutorialmaker.model.SpotlightShape
import com.pano.tutorialmaker.model.StepBranch
import com.pano.tutorialmaker.model.StepMode
import com.pano.tutorialmaker.model.TextPosition
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepPropertiesPanel(
    step: TutorialStep,
    onStepChanged: (TutorialStep) -> Unit,
    /** Other steps in the same section — populates the "jump to" picker for branches. */
    sectionSteps: List<TutorialStep> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Step Properties", style = MaterialTheme.typography.titleMedium)

        // Step mode
        Text("Mode", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = step.mode == StepMode.TOOLTIP,
                onClick = { onStepChanged(step.copy(mode = StepMode.TOOLTIP, scrollTrigger = null)) },
                label = { Text("Tooltip") }
            )
            FilterChip(
                selected = step.mode == StepMode.WALKTHROUGH,
                onClick = { onStepChanged(step.copy(mode = StepMode.WALKTHROUGH, scrollTrigger = null)) },
                label = { Text("Walkthrough") }
            )
            FilterChip(
                selected = step.mode == StepMode.SCROLL,
                onClick = {
                    onStepChanged(step.copy(
                        mode = StepMode.SCROLL,
                        scrollTrigger = step.scrollTrigger ?: ScrollTrigger(yFraction = 0.5f)
                    ))
                },
                label = { Text("Scroll") }
            )
        }

        // Target tag dropdown — filters as you type
        val availableTags = TutorialTagRegistry.elements.keys.toList()
        var tagExpanded by remember { mutableStateOf(false) }
        val tagFilter = step.target.tag ?: ""
        val filteredTags = remember(availableTags, tagFilter) {
            if (tagFilter.isEmpty()) availableTags
            else availableTags.filter { it.contains(tagFilter, ignoreCase = true) }
        }

        ExposedDropdownMenuBox(
            expanded = tagExpanded,
            onExpandedChange = { tagExpanded = it }
        ) {
            OutlinedTextField(
                value = step.target.tag ?: "",
                onValueChange = {
                    onStepChanged(step.copy(target = step.target.copy(tag = it.ifEmpty { null })))
                    tagExpanded = true
                },
                label = { Text("Target Tag") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                singleLine = true
            )
            if (filteredTags.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = tagExpanded,
                    onDismissRequest = { tagExpanded = false }
                ) {
                    filteredTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                onStepChanged(step.copy(target = step.target.copy(tag = tag)))
                                tagExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Spotlight shape
        Text("Spotlight Shape", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotlightShape.entries.forEach { shape ->
                FilterChip(
                    selected = step.spotlightShape == shape,
                    onClick = { onStepChanged(step.copy(spotlightShape = shape)) },
                    label = { Text(shape.name.lowercase().replace('_', ' ')) }
                )
            }
        }

        // Spotlight padding
        Text(
            "Spotlight Padding: ${step.spotlightPaddingDp.toInt()} dp",
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = step.spotlightPaddingDp,
            onValueChange = { onStepChanged(step.copy(spotlightPaddingDp = it)) },
            valueRange = 0f..40f
        )

        // Text content
        OutlinedTextField(
            value = step.text,
            onValueChange = { onStepChanged(step.copy(text = it)) },
            label = { Text("Step Text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5
        )

        // Help Mode text — optional override, falls back to Step Text when left blank
        OutlinedTextField(
            value = step.infoText,
            onValueChange = { onStepChanged(step.copy(infoText = it)) },
            label = { Text("Info Text (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5
        )
        Text(
            "Shown in Help Mode's on-demand tooltip. Leave blank to reuse Step Text.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Text position
        Text("Text Position", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextPosition.entries.forEach { pos ->
                FilterChip(
                    selected = step.textPosition == pos,
                    onClick = { onStepChanged(step.copy(textPosition = pos)) },
                    label = { Text(pos.name.lowercase()) }
                )
            }
        }

        // Text offset X
        Text(
            "Text Offset X: ${step.textOffsetXDp.toInt()} dp",
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = step.textOffsetXDp,
            onValueChange = { onStepChanged(step.copy(textOffsetXDp = it)) },
            valueRange = -200f..200f
        )

        // Text offset Y
        Text(
            "Text Offset Y: ${step.textOffsetYDp.toInt()} dp",
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = step.textOffsetYDp,
            onValueChange = { onStepChanged(step.copy(textOffsetYDp = it)) },
            valueRange = -200f..200f
        )

        // Dismiss on target click
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dismiss on target click", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = step.dismissOnTargetClick,
                onCheckedChange = { onStepChanged(step.copy(dismissOnTargetClick = it)) }
            )
        }

        // Advance condition + branches (Walkthrough mode only)
        if (step.mode == StepMode.WALKTHROUGH) {
            Text("Advance When", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = step.advanceCondition == AdvanceCondition.TAP,
                    onClick = { onStepChanged(step.copy(advanceCondition = AdvanceCondition.TAP)) },
                    label = { Text("Tapped") }
                )
                FilterChip(
                    selected = step.advanceCondition == AdvanceCondition.TARGET_DISMISSED,
                    onClick = { onStepChanged(step.copy(advanceCondition = AdvanceCondition.TARGET_DISMISSED)) },
                    label = { Text("Target closes") }
                )
            }
            if (step.advanceCondition == AdvanceCondition.TARGET_DISMISSED) {
                Text(
                    "Waits until the target disappears (e.g. a dialog that only closes on " +
                        "valid input) instead of advancing on the tap alone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text("Branches (optional)", style = MaterialTheme.typography.labelMedium)
            Text(
                "If the user taps one of these instead of the main target, jump to a " +
                    "different step instead of advancing normally.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            step.branches.forEachIndexed { index, branch ->
                BranchRow(
                    branch = branch,
                    sectionSteps = sectionSteps,
                    onChanged = { updated ->
                        val branches = step.branches.toMutableList()
                        branches[index] = updated
                        onStepChanged(step.copy(branches = branches))
                    },
                    onRemove = {
                        val branches = step.branches.toMutableList()
                        branches.removeAt(index)
                        onStepChanged(step.copy(branches = branches))
                    }
                )
            }
            OutlinedButton(onClick = { onStepChanged(step.copy(branches = step.branches + StepBranch())) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Branch")
            }
        }

        // Scroll trigger (only available in Scroll mode)
        if (step.mode != StepMode.SCROLL) return@Column
        Text("Scroll Trigger", style = MaterialTheme.typography.labelMedium)
        val scrollTrigger = step.scrollTrigger ?: ScrollTrigger(yFraction = 0.5f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = scrollTrigger.yFraction != null,
                onClick = {
                    val updated = if (scrollTrigger.yFraction != null)
                        scrollTrigger.copy(yFraction = null)
                    else
                        scrollTrigger.copy(yFraction = 0.5f)
                    onStepChanged(step.copy(scrollTrigger = updated))
                },
                label = { Text("Y line") }
            )
            FilterChip(
                selected = scrollTrigger.xFraction != null,
                onClick = {
                    val updated = if (scrollTrigger.xFraction != null)
                        scrollTrigger.copy(xFraction = null)
                    else
                        scrollTrigger.copy(xFraction = 0.5f)
                    onStepChanged(step.copy(scrollTrigger = updated))
                },
                label = { Text("X line") }
            )
        }
        Text(
            "Drag the line in the preview to reposition",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchRow(
    branch: StepBranch,
    sectionSteps: List<TutorialStep>,
    onChanged: (StepBranch) -> Unit,
    onRemove: () -> Unit
) {
    val availableTags = TutorialTagRegistry.elements.keys.toList()
    var tagExpanded by remember { mutableStateOf(false) }
    val filteredTags = remember(availableTags, branch.tag) {
        if (branch.tag.isEmpty()) availableTags
        else availableTags.filter { it.contains(branch.tag, ignoreCase = true) }
    }
    var stepExpanded by remember { mutableStateOf(false) }
    val selectedStepLabel = sectionSteps.indexOfFirst { it.id == branch.nextStepId }
        .takeIf { it != -1 }
        ?.let { "Step ${it + 1}" } ?: "Choose step"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = tagExpanded,
            onExpandedChange = { tagExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = branch.tag,
                onValueChange = {
                    onChanged(branch.copy(tag = it))
                    tagExpanded = true
                },
                label = { Text("If tag") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
            )
            if (filteredTags.isNotEmpty()) {
                ExposedDropdownMenu(expanded = tagExpanded, onDismissRequest = { tagExpanded = false }) {
                    filteredTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                onChanged(branch.copy(tag = tag))
                                tagExpanded = false
                            }
                        )
                    }
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = stepExpanded,
            onExpandedChange = { stepExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedStepLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Go to") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stepExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = stepExpanded, onDismissRequest = { stepExpanded = false }) {
                if (sectionSteps.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No steps in this section") },
                        onClick = { stepExpanded = false },
                        enabled = false
                    )
                } else {
                    sectionSteps.forEachIndexed { idx, s ->
                        DropdownMenuItem(
                            text = { Text("Step ${idx + 1}: ${s.text.ifBlank { "(empty)" }}") },
                            onClick = {
                                onChanged(branch.copy(nextStepId = s.id))
                                stepExpanded = false
                            }
                        )
                    }
                }
            }
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove branch")
        }
    }
}
