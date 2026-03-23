package com.pano.tutorialmaker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.editor.EditorOverlay
import com.pano.tutorialmaker.io.TutorialFileManager

/**
 * Wraps app content with TutorialMaker editor and player support.
 *
 * Usage:
 * ```
 * TutorialMaker(basePath = filesDir.absolutePath) {
 *     // your app content
 *     MyAppScreen()
 * }
 * ```
 *
 * @param basePath Directory for saving/loading tutorial JSON files.
 *                 On Android use `context.filesDir.absolutePath`.
 *                 On desktop use `"."` or a preferred directory.
 * @param enabled  When true, shows editor controls. Set to false for production builds.
 * @param content  Your app's composable content.
 */
@Composable
fun TutorialMaker(
    basePath: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val fileManager = remember(basePath) { TutorialFileManager(basePath = basePath) }
    var showEditor by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // App content
        content()

        if (enabled) {
            if (showEditor) {
                // Editor overlay on top of the app
                EditorOverlay(
                    fileManager = fileManager,
                    onClose = { showEditor = false }
                )
            } else {
                // Edit FAB
                SmallFloatingActionButton(
                    onClick = { showEditor = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 96.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Tutorial")
                }
            }
        }
    }
}
