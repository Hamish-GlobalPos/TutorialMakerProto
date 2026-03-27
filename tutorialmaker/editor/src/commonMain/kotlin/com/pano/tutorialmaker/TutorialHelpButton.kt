package com.pano.tutorialmaker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A self-contained help button that toggles help mode when tapped.
 * Place it anywhere inside a [TutorialMaker] block — no extra wiring needed.
 *
 * Usage:
 * ```
 * TopAppBar(
 *     actions = { TutorialHelpButton() }
 * )
 * ```
 */
@Composable
fun TutorialHelpButton(modifier: Modifier = Modifier) {
    val ctx = LocalTutorialContext.current ?: return
    if (ctx.tutorial == null || !ctx.helpEnabled) return

    IconButton(
        onClick = { ctx.setHelpModeActive(!ctx.helpModeActive) },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = "Help",
            tint = if (ctx.helpModeActive)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
