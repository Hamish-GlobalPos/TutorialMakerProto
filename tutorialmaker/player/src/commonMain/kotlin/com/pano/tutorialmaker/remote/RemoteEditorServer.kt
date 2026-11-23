package com.pano.tutorialmaker.remote

/**
 * Debug-only socket server that streams snapshots and frames to the PC editor and receives
 * input from it. The PC reaches it through `adb forward tcp:PORT tcp:PORT`.
 * Only implemented on Android; no-op elsewhere.
 */
expect class RemoteEditorServer(port: Int) {
    val hasClients: Boolean

    /** Listeners are called on a background thread for each message received from the PC. */
    fun addListener(listener: (type: Int, payload: ByteArray) -> Unit)
    fun removeListener(listener: (type: Int, payload: ByteArray) -> Unit)

    fun start()
    fun stop()

    /** Sends a message to all clients. [MSG_SNAPSHOT] is also replayed to clients that connect later. */
    fun publish(type: Int, payload: ByteArray)
}
