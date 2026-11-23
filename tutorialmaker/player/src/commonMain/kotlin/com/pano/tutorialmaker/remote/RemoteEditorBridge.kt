package com.pano.tutorialmaker.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.pano.tutorialmaker.io.TutorialFileManager
import com.pano.tutorialmaker.io.TutorialProgressManager
import com.pano.tutorialmaker.tagging.TutorialTagRegistry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

/** Tutorial ids become file names, so refuse anything that could escape the tutorials folder. */
private fun isSafeId(id: String) = id.isNotEmpty() && id.all { it.isLetterOrDigit() || it == '_' || it == '-' }

/**
 * Streams the live tag layout and screen frames to the PC editor, injects the PC's input, and
 * syncs tutorial files with it, while composed. Add once, near the app root.
 *
 * @param onTutorialChanged Called (on a background thread) after the PC pushed, deleted or asked
 *   to preview a tutorial, so the app can reload it.
 */
@Composable
fun RemoteEditorBridge(
    fileManager: TutorialFileManager,
    progressManager: TutorialProgressManager,
    onTutorialChanged: (tutorialId: String?) -> Unit,
    port: Int = REMOTE_EDITOR_DEFAULT_PORT
) {
    val server = remember(port) { RemoteEditorServer(port) }
    DisposableEffect(server) {
        server.start()
        onDispose { server.stop() }
    }

    val currentOnChanged by rememberUpdatedState(onTutorialChanged)
    DisposableEffect(server, fileManager, progressManager) {
        val listener: (Int, ByteArray) -> Unit = { type, payload ->
            when (type) {
                MSG_PULL -> {
                    val files = fileManager.listTutorials().mapNotNull { id ->
                        fileManager.readRaw(id)?.let { RemoteTutorialFile(id, it) }
                    }
                    server.publish(MSG_TUTORIALS, RemoteJson.encodeToString(RemoteTutorials(files)).encodeToByteArray())
                }
                MSG_PUSH -> {
                    val file = RemoteJson.decodeFromString<RemoteTutorialFile>(payload.decodeToString())
                    if (isSafeId(file.id)) {
                        if (file.json != null) fileManager.writeRaw(file.id, file.json)
                        else fileManager.deleteTutorial(file.id)
                        currentOnChanged(file.id.takeIf { file.json != null })
                    }
                }
                MSG_COMMAND -> {
                    val command = RemoteJson.decodeFromString<RemoteCommand>(payload.decodeToString())
                    val id = command.tutorialId
                    if (command.cmd == "preview" && id != null && isSafeId(id)) {
                        command.sectionIds.forEach { progressManager.resetSection(id, it) }
                        currentOnChanged(id)
                    }
                }
            }
        }
        server.addListener(listener)
        onDispose { server.removeListener(listener) }
    }

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current.density

    LaunchedEffect(server) {
        snapshotFlow {
            RemoteSnapshot(
                widthPx = windowInfo.containerSize.width,
                heightPx = windowInfo.containerSize.height,
                density = density,
                tags = TutorialTagRegistry.elements.entries
                    .sortedBy { it.key }
                    .map { (tag, r) -> RemoteTag(tag, r.left, r.top, r.right, r.bottom) },
                screens = TutorialTagRegistry.screens.keys.sorted()
            )
        }
            .distinctUntilChanged()
            .map { RemoteJson.encodeToString(it).encodeToByteArray() }
            .collect { server.publish(MSG_SNAPSHOT, it) }
    }

    RemoteFrameAndInput(server)
}

/** Platform part of the bridge: frame capture and input injection. Android only. */
@Composable
internal expect fun RemoteFrameAndInput(server: RemoteEditorServer)
