package com.pano.tutorialmaker.remote

import androidx.compose.runtime.Composable

actual class RemoteEditorServer actual constructor(port: Int) {
    actual fun addListener(listener: (type: Int, payload: ByteArray) -> Unit) {}
    actual fun removeListener(listener: (type: Int, payload: ByteArray) -> Unit) {}
    actual val hasClients: Boolean get() = false
    actual fun start() {}
    actual fun stop() {}
    actual fun publish(type: Int, payload: ByteArray) {}
}

@Composable
internal actual fun RemoteFrameAndInput(server: RemoteEditorServer) {}
