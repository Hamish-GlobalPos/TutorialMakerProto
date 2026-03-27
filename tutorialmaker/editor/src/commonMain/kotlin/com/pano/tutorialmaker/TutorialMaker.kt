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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pano.tutorialmaker.editor.EditorOverlay
import com.pano.tutorialmaker.io.TutorialFileManager
import com.pano.tutorialmaker.io.TutorialProgressManager
import com.pano.tutorialmaker.io.TutorialSettingsManager
import com.pano.tutorialmaker.model.Tutorial
import com.pano.tutorialmaker.model.TutorialSection
import com.pano.tutorialmaker.model.TutorialStep
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
 *
 * @param resetAndReplay Resets all progress for the active tutorial and replays on next launch.
 * @param playOnEveryStart Whether the tutorial resets and replays on every app launch.
 * @param setPlayOnEveryStart Persists the playOnEveryStart setting.
 * @param helpModeActive Whether help mode (tag highlight overlay) is currently active.
 * @param setHelpModeActive Show or hide help mode programmatically.
 */
data class TutorialContext(
    val fileManager: TutorialFileManager,
    val progressManager: TutorialProgressManager,
    val tutorial: Tutorial?,
    val activeTutorialState: ActiveTutorialState,
    val version: Int = 0,
    val playOnEveryStart: Boolean = false,
    val setPlayOnEveryStart: (Boolean) -> Unit = {},
    val resetAndReplay: () -> Unit = {},
    val helpEnabled: Boolean = true,
    val setHelpEnabled: (Boolean) -> Unit = {},
    val helpModeActive: Boolean = false,
    val setHelpModeActive: (Boolean) -> Unit = {}
)

val LocalTutorialContext = compositionLocalOf<TutorialContext?> { null }

/**
 * Wraps app content with TutorialMaker editor, player, and help mode support.
 *
 * Tutorial settings (play on every start) are persisted automatically and can be
 * read/written from any composable via [LocalTutorialContext].
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
 * @param helpEnabled When true, help mode overlay is available.
 * @param showHelpButton When true, shows the built-in info FAB. Set to false to use your own [TutorialHelpButton] instead.
 * @param tutorialId ID of the tutorial to load for help mode and section triggers.
 * @param content Your app's composable content.
 */
@Composable
fun TutorialMaker(
    basePath: String,
    editorEnabled: Boolean = true,
    helpEnabled: Boolean = true,
    showHelpButton: Boolean = true,
    tutorialId: String? = null,
    content: @Composable () -> Unit
) {
    val fileManager = remember(basePath) { TutorialFileManager(basePath = basePath) }
    val progressManager = remember(basePath) { TutorialProgressManager(basePath = basePath) }
    val settingsManager = remember(basePath) { TutorialSettingsManager(basePath = basePath) }
    var showEditor by remember { mutableStateOf(false) }
    var helpEnabledState by remember { mutableStateOf(helpEnabled) }
    var helpModeActive by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var lastEditorTutorialId by remember { mutableStateOf<String?>(null) }
    var playOnEveryStart by remember { mutableStateOf(settingsManager.getPlayOnEveryStart()) }
    var helpActiveStep by remember { mutableStateOf<TutorialStep?>(null) }
    var helpActiveRect by remember { mutableStateOf<Rect?>(null) }

    val tutorial = remember(tutorialId, reloadKey) {
        val resolvedId = tutorialId ?: lastEditorTutorialId ?: fileManager.listTutorials().firstOrNull()
        resolvedId?.let { fileManager.loadTutorial(it) }
    }

    val tagStepMap = remember(tutorial) {
        val map = mutableMapOf<String, TutorialStep>()
        for (section in tutorial?.sections ?: emptyList()) {
            for (step in section.steps) {
                val tag = step.target.tag ?: continue
                if (tag !in map) map[tag] = step
            }
        }
        map
    }

    val activeTutorialState = remember { ActiveTutorialState() }

    fun resetAndReplay() {
        val t = tutorial ?: return
        progressManager.reset(t.id)
    }

    // Reset help tooltip when help mode is turned off
    LaunchedEffect(helpModeActive) {
        if (!helpModeActive) {
            helpActiveStep = null
            helpActiveRect = null
        }
    }

    // Auto-play on start: play incomplete sections (or all if playOnEveryStart)
    LaunchedEffect(tutorial?.id) {
        val t = tutorial ?: return@LaunchedEffect
        if (playOnEveryStart) {
            resetAndReplay()
        } else {
            val incomplete = t.sections.filter { !progressManager.isSectionCompleted(t.id, it.id) }
            if (incomplete.isNotEmpty()) activeTutorialState.playQueue(incomplete)
        }
    }

    val tutorialContext = remember(tutorial, reloadKey, playOnEveryStart, helpEnabledState, helpModeActive) {
        TutorialContext(
            fileManager = fileManager,
            progressManager = progressManager,
            tutorial = tutorial,
            activeTutorialState = activeTutorialState,
            version = reloadKey,
            playOnEveryStart = playOnEveryStart,
            setPlayOnEveryStart = { value ->
                playOnEveryStart = value
                settingsManager.setPlayOnEveryStart(value)
            },
            resetAndReplay = ::resetAndReplay,
            helpEnabled = helpEnabledState,
            setHelpEnabled = { helpEnabledState = it },
            helpModeActive = helpModeActive,
            setHelpModeActive = { helpModeActive = it }
        )
    }

    // Observe play requests from SectionTrigger and feed to ActiveTutorialState
    val playRequest = TutorialTagRegistry.pendingPlayRequest
    if (playRequest != null && !activeTutorialState.isPlaying) {
        TutorialTagRegistry.consumePlayRequest()
        activeTutorialState.playQueue(playRequest)
    }

    // Captured by the pointerInput coroutine below via rememberUpdatedState
    val helpTapEnabled by rememberUpdatedState(
        helpModeActive && helpEnabledState && tutorial != null && !activeTutorialState.isPlaying
    )
    val currentTagStepMap by rememberUpdatedState(tagStepMap)

    CompositionLocalProvider(LocalTutorialContext provides tutorialContext) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press && helpTapEnabled && helpActiveStep == null) {
                                val offset = event.changes.firstOrNull()?.position ?: continue
                                val map = currentTagStepMap
                                val hit = TutorialTagRegistry.elements.entries
                                    .find { (tag, rect) -> rect.contains(offset) && tag in map }
                                if (hit != null) {
                                    helpActiveStep = map[hit.key]
                                    helpActiveRect = hit.value
                                }
                            }
                        }
                    }
                }
        ) {
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
                        progressManager.markSectionCompleted(tutorial.id, activeSection.id)
                        activeTutorialState.onComplete()
                    }
                )
            }

            // Help mode overlay
            if (helpEnabledState && tutorial != null && !activeTutorialState.isPlaying) {
                HelpMode(
                    tagStepMap = tagStepMap,
                    enabled = helpModeActive,
                    activeStep = helpActiveStep,
                    activeRect = helpActiveRect,
                    onDismissStep = {
                        helpActiveStep = null
                        helpActiveRect = null
                    }
                )
            }

            // Editor overlay
            if (editorEnabled && showEditor) {
                EditorOverlay(
                    fileManager = fileManager,
                    progressManager = progressManager,
                    onClose = { loadedId ->
                        lastEditorTutorialId = loadedId
                        showEditor = false
                        reloadKey++
                    },
                    onPreviewStart = { id ->
                        lastEditorTutorialId = id
                        reloadKey++
                    },
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
                    if (helpEnabledState && showHelpButton && tutorial != null) {
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
