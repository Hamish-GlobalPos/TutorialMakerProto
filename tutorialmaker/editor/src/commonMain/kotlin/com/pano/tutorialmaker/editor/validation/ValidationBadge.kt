package com.pano.tutorialmaker.editor.validation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Warning icon with a badge count. Empty [issues] renders a dim, inert icon — nothing to open.
 * Clicking an issue in the menu jumps the editor to that section/step via [onJump].
 */
@Composable
fun ValidationBadge(
    issues: List<ValidationIssue>,
    onJump: (sectionIndex: Int, stepIndex: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        BadgedBox(
            badge = {
                if (issues.isNotEmpty()) {
                    Badge { Text(issues.size.toString()) }
                }
            }
        ) {
            IconButton(onClick = { if (issues.isNotEmpty()) expanded = true }) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = if (issues.isEmpty()) "No validation issues" else "Validation issues",
                    tint = if (issues.isNotEmpty())
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            issues.forEach { issue ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "Section ${issue.sectionIndex + 1}" +
                                (issue.stepIndex?.let { " · Step ${it + 1}" } ?: "") +
                                ": ${issue.message}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {
                        onJump(issue.sectionIndex, issue.stepIndex)
                        expanded = false
                    }
                )
            }
        }
    }
}
