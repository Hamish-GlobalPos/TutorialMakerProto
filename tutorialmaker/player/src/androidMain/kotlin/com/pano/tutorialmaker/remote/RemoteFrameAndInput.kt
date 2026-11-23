package com.pano.tutorialmaker.remote

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.coroutineContext

private const val FRAME_WIDTH = 720
private const val JPEG_QUALITY = 70
private const val FRAME_INTERVAL_MS = 33L
private const val IDLE_INTERVAL_MS = 250L

@Composable
internal actual fun RemoteFrameAndInput(server: RemoteEditorServer) {
    val view = LocalView.current

    // PC -> device: inject touches into the window.
    DisposableEffect(view, server) {
        var downTime = 0L
        val listener: (Int, ByteArray) -> Unit = { type, payload ->
            if (type == MSG_INPUT) {
                runCatching {
                    val input = RemoteJson.decodeFromString<RemoteInput>(payload.decodeToString())
                    view.post {
                        val now = SystemClock.uptimeMillis()
                        if (input.action == MotionEvent.ACTION_DOWN) downTime = now
                        val event = MotionEvent.obtain(downTime, now, input.action, input.x, input.y, 0)
                        view.rootView.dispatchTouchEvent(event)
                        event.recycle()
                    }
                }
            }
        }
        server.addListener(listener)
        onDispose { server.removeListener(listener) }
    }

    // Device -> PC: scaled JPEG frames of the window, only while someone is listening.
    LaunchedEffect(view, server) {
        val window = view.context.findActivity()?.window
        var bitmap: Bitmap? = null
        while (coroutineContext.isActive) {
            val root = view.rootView
            if (!server.hasClients || root.width == 0 || root.height == 0) {
                delay(IDLE_INTERVAL_MS)
                continue
            }
            val w = FRAME_WIDTH
            val h = (FRAME_WIDTH * root.height.toFloat() / root.width).toInt().coerceAtLeast(1)
            val bmp = bitmap?.takeIf { it.width == w && it.height == h }
                ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap = it }

            if (capture(root, window, bmp)) {
                val jpeg = withContext(Dispatchers.Default) {
                    ByteArrayOutputStream(64 * 1024).also { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                        .toByteArray()
                }
                server.publish(MSG_FRAME, jpeg)
            }
            delay(FRAME_INTERVAL_MS)
        }
    }
}

private suspend fun capture(root: View, window: Window?, dest: Bitmap): Boolean =
    if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        suspendCancellableCoroutine { cont ->
            runCatching {
                PixelCopy.request(window, dest, { result -> if (cont.isActive) cont.resume(result == PixelCopy.SUCCESS) },
                    Handler(Looper.getMainLooper()))
            }.onFailure { if (cont.isActive) cont.resume(false) }
        }
    } else {
        // Pre-O fallback: software draw of the view tree (misses dialogs/popups).
        val canvas = Canvas(dest)
        canvas.scale(dest.width / root.width.toFloat(), dest.height / root.height.toFloat())
        root.draw(canvas)
        true
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
