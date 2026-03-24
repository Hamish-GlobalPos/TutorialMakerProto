package com.pano.tutorialmaker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.editor.EditorOverlay
import com.pano.tutorialmaker.io.TutorialFileManager
import com.pano.tutorialmaker.io.TutorialProgressManager
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.model.TutorialSection
import com.pano.tutorialmaker.player.HelpMode
import com.pano.tutorialmaker.player.TutorialPlayer
import com.pano.tutorialmaker.tagging.TutorialTagRegistry

/**
 * Shared state for the active tutorial player.
 * SectionTrigger sets the active section, TutorialMaker plays it.
 */
class ActiveTutorialState {
    var activeSection by mutableStateOf<TutorialSection?>(null)
    var sectionQueue by mutableStateOf<List<TutorialSection>>(emptyList())

    fun play(section: TutorialSection) {
        activeSection = section
    }

    fun playQueue(sections: List<TutorialSection>) {
        if (sections.isEmpty()) return
        activeSection = sections.first()
        sectionQueue = sections.drop(1)
    }

    fun onComplete() {
        if (sectionQueue.isNotEmpty()) {
            activeSection = sectionQueue.first()
            sectionQueue = sectionQueue.drop(1)
        } else {
            activeSection = null
        }
    }

    val isPlaying: Boolean get() = activeSection != null
}

/**
 * Context provided to child composables for tutorial features.
 */
data class TutorialContext(
    val fileManager: TutorialFileManager,
    val progressManager: TutorialProgressManager,
    val tutorial: Tutorial?,
    val activeTutorialState: ActiveTutorialState,
    val version: Int = 0
)

val LocalTutorialContext = compositionLocalOf<TutorialContext?> { null }

/**
 * Wraps app content with TutorialMaker editor, player, and help mode support.
 *
 * Usage:
 * ```
 * TutorialMaker(basePath = filesDir.absolutePath) {
 *     MyAppScreen()
 * }
 * ```
 *
 * @param basePath Directory for saving/loading tutorial JSON files.
 * @param editorEnabled When true, shows editor controls. Set to false for production.
 * @param helpEnabled When true, shows help mode button. Can be true in production.
 * @param tutorialId ID of the tutorial to load for help mode and section triggers.
 * @param content Your app's composable content.
 */
@Composable
fun TutorialMaker(
    basePath: String,
    editorEnabled: Boolean = true,
    helpEnabled: Boolean = true,
    tutorialId: String? = null,
    content: @Composable () -> Unit
) {
    val fileManager = remember(basePath) { TutorialFileManager(basePath = basePath) }
    val progressManager = remember(basePath) { TutorialProgressManager(basePath = basePath) }
    var showEditor by remember { mutableStateOf(false) }
    var helpModeActive by remember { mutableStateOf(false) }
    // Incremented when editor closes to force tutorial reload
    var reloadKey by remember { mutableStateOf(0) }

    // Load the tutorial for help mode / section triggers
    // Reloads when tutorialId changes or editor closes
    val tutorial = remember(tutorialId, reloadKey) {
        val ids = fileManager.listTutorials()
        println("TM_LOG [TutorialMaker] reloadKey=$reloadKey, tutorialId=$tutorialId, available=$ids")
        val t = if (tutorialId != null) {
            fileManager.loadTutorial(tutorialId)
        } else {
            ids.firstOrNull()?.let { fileManager.loadTutorial(it) }
        }
        println("TM_LOG [TutorialMaker] loaded tutorial: id=${t?.id}, sections=${t?.sections?.size}, sectionDetails=${t?.sections?.map { "${it.id}:${it.triggerType}:screenTag=${it.screenTag}" }}")
        t
    }

    val activeTutorialState = remember { ActiveTutorialState() }

    val tutorialContext = remember(fileManager, progressManager, tutorial, reloadKey) {
        println("TM_LOG [TutorialMaker] creating TutorialContext version=$reloadKey, tutorial=${tutorial?.id}")
        TutorialContext(fileManager, progressManager, tutorial, activeTutorialState, version = reloadKey)
    }

    // Observe play requests from SectionTrigger and feed to ActiveTutorialState
    val playRequest = TutorialTagRegistry.pendingPlayRequest
    if (playRequest != null && !activeTutorialState.isPlaying) {
        TutorialTagRegistry.consumePlayRequest()
        activeTutorialState.playQueue(playRequest)
    }

    CompositionLocalProvider(LocalTutorialContext provides tutorialContext) {
        Box(modifier = Modifier.fillMaxSize()) {
            // App content
            content()

            // Active tutorial player — lives at root level, survives screen navigation
            val activeSection = activeTutorialState.activeSection
            if (activeSection != null && tutorial != null) {
                val sectionTutorial = remember(activeSection) {
                    tutorial.copy(sections = listOf(activeSection))
                }
                TutorialPlayer(
                    tutorial = sectionTutorial,
                    onComplete = {
                        println("TM_LOG [TutorialMaker] section ${activeSection.id} completed")
                        progressManager.markSectionCompleted(tutorial.id, activeSection.id)
                        activeTutorialState.onComplete()
                    }
                )
            }

            // Help mode overlay
            if (helpEnabled && tutorial != null && !activeTutorialState.isPlaying) {
                HelpMode(
                    tutorial = tutorial,
                    enabled = helpModeActive,
                    onDismiss = { helpModeActive = false }
                )
            }

            // Editor overlay
            if (editorEnabled && showEditor) {
                EditorOverlay(
                    fileManager = fileManager,
                    progressManager = progressManager,
                    onClose = {
                        showEditor = false
                        reloadKey++
                    },
                    onPreviewStart = { reloadKey++ },
                    onPreviewEnd = { reloadKey++ }
                )
            }

            // FABs
            if (!showEditor) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editorEnabled) {
                        SmallFloatingActionButton(
                            onClick = { showEditor = true },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Tutorial")
                        }
                    }
                    if (helpEnabled && tutorial != null) {
                        SmallFloatingActionButton(
                            onClick = { helpModeActive = !helpModeActive },
                            containerColor = if (helpModeActive)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Help Mode")
                        }
                    }
                }
            }
        }
    }
}
