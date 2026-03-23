package com.pano.tutorialmaker.editor.properties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.pano.tutorialmaker.model.SpotlightShape
import com.pano.tutorialmaker.model.TextPosition
import com.pano.tutorialmaker.model.TutorialStep
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepPropertiesPanel(
    step: TutorialStep,
    onStepChanged: (TutorialStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Step Properties", style = MaterialTheme.typography.titleMedium)

        // Target tag dropdown
        val availableTags = TutorialTagRegistry.elements.keys.toList()
        var tagExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = tagExpanded,
            onExpandedChange = { tagExpanded = it }
        ) {
            OutlinedTextField(
                value = step.target.tag ?: "",
                onValueChange = {
                    onStepChanged(step.copy(target = step.target.copy(tag = it.ifEmpty { null })))
                },
                label = { Text("Target Tag") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = tagExpanded,
                onDismissRequest = { tagExpanded = false }
            ) {
                availableTags.forEach { tag ->
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
    }
}
