package com.pano.tutorialmaker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

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

    // Help mode swallows taps on the app content; this button must stay tappable so the user can
    // turn help mode off again, so it registers its bounds as exempt from that.
    val exemptionKey = remember { Any() }
    DisposableEffect(exemptionKey) {
        onDispose { TutorialTagRegistry.tapExemptions.remove(exemptionKey) }
    }

    IconButton(
        onClick = { ctx.setHelpModeActive(!ctx.helpModeActive) },
        modifier = modifier.onGloballyPositioned {
            TutorialTagRegistry.tapExemptions[exemptionKey] = it.boundsInRoot()
        }
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
