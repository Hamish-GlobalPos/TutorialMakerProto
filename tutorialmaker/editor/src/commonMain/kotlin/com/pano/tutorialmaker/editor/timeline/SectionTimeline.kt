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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.model.TutorialSection

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
