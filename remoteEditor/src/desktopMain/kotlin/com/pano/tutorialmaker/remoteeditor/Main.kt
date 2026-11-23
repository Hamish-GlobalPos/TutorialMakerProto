package com.pano.tutorialmaker.remoteeditor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pano.tutorialmaker.io.TutorialFileManager
import java.io.File

/** Optional first argument: folder to keep the local copy of the device's tutorials in. */
fun main(args: Array<String>) = application {
    val workspacePath = args.firstOrNull()
        ?: File(System.getProperty("user.home"), "TutorialMakerRemote").path
    val session = remember {
        RemoteSession(TutorialFileManager(workspacePath)).also { it.start() }
    }
    val close = { session.stop(); exitApplication() }

    // Interactive window: mouse input is forwarded to the app on the device.
    Window(
        onCloseRequest = close,
        title = "Device (interactive)",
        state = rememberWindowState(size = DpSize(520.dp, 900.dp), position = WindowPosition(40.dp, 40.dp))
    ) {
        MaterialTheme {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Text(session.status)
                Spacer(Modifier.height(8.dp))
                MirrorView(session, Modifier.weight(1f).fillMaxWidth()) { scale ->
                    Box(Modifier.fillMaxSize().forwardInputToDevice(scale, session, enabled = session.snapshot != null))
                }
            }
        }
    }

    // Editor window: the real editor tools drawn over the same mirror. Never touches the device
    // directly; Save pushes the tutorial over.
    Window(
        onCloseRequest = close,
        title = "Tutorial Editor  -  $workspacePath",
        state = rememberWindowState(size = DpSize(1300.dp, 950.dp), position = WindowPosition(600.dp, 40.dp))
    ) {
        MaterialTheme { EditorWindowContent(session) }
    }
}

/**
 * The latest device frame, aspect-fitted, with [overlay] drawn on top. The overlay is given the
 * scale from device (app root) pixels to the displayed pixels.
 */
@Composable
fun MirrorView(
    session: RemoteSession,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.(scale: Float) -> Unit
) {
    val frame = session.frame
    Box(modifier, contentAlignment = Alignment.Center) {
        if (frame == null) {
            Text("Waiting for first frame...")
            return@Box
        }
        BoxWithConstraints(Modifier.fillMaxHeight().aspectRatio(frame.width.toFloat() / frame.height)) {
            // Frames are downscaled JPEGs, so map via the app's reported size, not the image's.
            val deviceWidth = session.snapshot?.widthPx ?: frame.width
            val scale = with(LocalDensity.current) { maxWidth.toPx() } / deviceWidth
            Image(frame, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            overlay(scale)
        }
    }
}

/** Forwards mouse press/drag/release (and wheel) to the app as touch events. */
private fun Modifier.forwardInputToDevice(scale: Float, session: RemoteSession, enabled: Boolean): Modifier =
    pointerInput(scale, enabled) {
        if (!enabled) return@pointerInput
        awaitPointerEventScope {
            var down = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                val x = change.position.x / scale
                val y = change.position.y / scale
                when (event.type) {
                    PointerEventType.Press -> { down = true; session.sendInput(ACTION_DOWN, x, y) }
                    PointerEventType.Move -> if (down) session.sendInput(ACTION_MOVE, x, y)
                    PointerEventType.Release -> if (down) { down = false; session.sendInput(ACTION_UP, x, y) }
                    PointerEventType.Scroll -> {
                        // Wheel -> a short synthetic swipe (content follows the finger).
                        val d = Offset(-change.scrollDelta.x, -change.scrollDelta.y) * WHEEL_PX
                        session.sendInput(ACTION_DOWN, x, y)
                        session.sendInput(ACTION_MOVE, x + d.x / 2, y + d.y / 2)
                        session.sendInput(ACTION_MOVE, x + d.x, y + d.y)
                        session.sendInput(ACTION_UP, x + d.x, y + d.y)
                    }
                }
            }
        }
    }

private const val ACTION_DOWN = 0
private const val ACTION_UP = 1
private const val ACTION_MOVE = 2
private const val WHEEL_PX = 150f
