package com.pano.tutorialmaker.editor.timeline

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.TriggerType
import com.pano.tutorialmaker.model.TutorialSection
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

@Composable
fun SectionTimeline(
    sections: List<TutorialSection>,
    selectedSectionIndex: Int,
    selectedStepIndex: Int,
    onSelectSection: (Int) -> Unit,
    onSelectStep: (sectionIndex: Int, stepIndex: Int) -> Unit,
    onAddSection: () -> Unit,
    onRemoveSection: (Int) -> Unit,
    onAddStep: (sectionIndex: Int) -> Unit,
    onRemoveStep: (sectionIndex: Int, stepIndex: Int) -> Unit,
    onMoveStep: (sectionIndex: Int, fromIndex: Int, toIndex: Int) -> Unit,
    onSectionChanged: (sectionIndex: Int, TutorialSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section chips row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sections.forEachIndexed { index, section ->
                    FilterChip(
                        selected = index == selectedSectionIndex,
                        onClick = { onSelectSection(index) },
                        label = { Text(section.title.ifEmpty { "Section ${index + 1}" }) },
                        trailingIcon = if (index == selectedSectionIndex) {
                            {
                                IconButton(
                                    onClick = { onRemoveSection(index) },
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove section",
                                        modifier = Modifier.padding(0.dp)
                                    )
                                }
                            }
                        } else null
                    )
                }
                AssistChip(
                    onClick = onAddSection,
                    label = { Text("Add Section") },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                )
            }

            // Section config row
            if (selectedSectionIndex in sections.indices) {
                val section = sections[selectedSectionIndex]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Section name
                    OutlinedTextField(
                        value = section.title,
                        onValueChange = {
                            onSectionChanged(selectedSectionIndex, section.copy(title = it))
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Trigger type
                    FilterChip(
                        selected = section.triggerType == TriggerType.SCREEN,
                        onClick = {
                            onSectionChanged(selectedSectionIndex, section.copy(triggerType = TriggerType.SCREEN))
                        },
                        label = { Text("Screen") }
                    )
                    FilterChip(
                        selected = section.triggerType == TriggerType.ELEMENT,
                        onClick = {
                            onSectionChanged(selectedSectionIndex, section.copy(triggerType = TriggerType.ELEMENT))
                        },
                        label = { Text("Element") }
                    )

                    // Screen ID dropdown — shows registered screen tags from SectionTrigger
                    if (section.triggerType == TriggerType.SCREEN || section.triggerType == TriggerType.ELEMENT) {
                        ScreenDropdown(
                            value = section.screenTag ?: "",
                            label = "Screen",
                            onValueChanged = {
                                onSectionChanged(selectedSectionIndex, section.copy(screenTag = it.ifEmpty { null }))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Element tag dropdown — shows registered tutorialTag elements
                    if (section.triggerType == TriggerType.ELEMENT) {
                        TagDropdown(
                            value = section.elementTag ?: "",
                            label = "Element Tag",
                            onValueChanged = {
                                onSectionChanged(selectedSectionIndex, section.copy(elementTag = it.ifEmpty { null }))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Steps row for selected section
            if (selectedSectionIndex in sections.indices) {
                val section = sections[selectedSectionIndex]
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    section.steps.forEachIndexed { stepIdx, step ->
                        StepCard(
                            step = step,
                            stepNumber = stepIdx + 1,
                            isSelected = stepIdx == selectedStepIndex,
                            canMoveLeft = stepIdx > 0,
                            canMoveRight = stepIdx < section.steps.size - 1,
                            onClick = { onSelectStep(selectedSectionIndex, stepIdx) },
                            onDelete = { onRemoveStep(selectedSectionIndex, stepIdx) },
                            onMoveLeft = { onMoveStep(selectedSectionIndex, stepIdx, stepIdx - 1) },
                            onMoveRight = { onMoveStep(selectedSectionIndex, stepIdx, stepIdx + 1) }
                        )
                    }
                    FilledTonalButton(
                        onClick = { onAddStep(selectedSectionIndex) }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Step")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenDropdown(
    value: String,
    label: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableScreens = TutorialTagRegistry.screens.keys.toList()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChanged(it) },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (availableScreens.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No screens registered") },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                availableScreens.forEach { screen ->
                    DropdownMenuItem(
                        text = { Text(screen) },
                        onClick = {
                            onValueChanged(screen)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagDropdown(
    value: String,
    label: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableTags = TutorialTagRegistry.elements.keys.toList()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChanged(it) },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableTags.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(tag) },
                    onClick = {
                        onValueChanged(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}
