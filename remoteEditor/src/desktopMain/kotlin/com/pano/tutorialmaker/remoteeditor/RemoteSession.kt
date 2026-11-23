package com.pano.tutorialmaker.remoteeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.pano.tutorialmaker.io.TutorialFileManager
import com.pano.tutorialmaker.remote.MSG_COMMAND
import com.pano.tutorialmaker.remote.MSG_FRAME
import com.pano.tutorialmaker.remote.MSG_INPUT
import com.pano.tutorialmaker.remote.MSG_PULL
import com.pano.tutorialmaker.remote.MSG_PUSH
import com.pano.tutorialmaker.remote.MSG_SNAPSHOT
import com.pano.tutorialmaker.remote.MSG_TUTORIALS
import com.pano.tutorialmaker.remote.REMOTE_EDITOR_DEFAULT_PORT
import com.pano.tutorialmaker.remote.RemoteCommand
import com.pano.tutorialmaker.remote.RemoteInput
import com.pano.tutorialmaker.remote.RemoteJson
import com.pano.tutorialmaker.remote.RemoteSnapshot
import com.pano.tutorialmaker.remote.RemoteTutorialFile
import com.pano.tutorialmaker.remote.RemoteTutorials
import kotlinx.serialization.encodeToString
import org.jetbrains.skia.Image
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlin.concurrent.thread

/** Owns the adb forward and the device socket: receives snapshots/frames, sends input. */
class RemoteSession(
    /** Local copy of the device's tutorials; the editor reads and writes here. */
    val workspace: TutorialFileManager,
    private val port: Int = REMOTE_EDITOR_DEFAULT_PORT
) {
    var snapshot by mutableStateOf<RemoteSnapshot?>(null)
        private set
    var frame by mutableStateOf<ImageBitmap?>(null)
        private set
    var status by mutableStateOf("Starting...")
        private set

    /** Bumped each time tutorials were pulled from the device into [workspace]. */
    var tutorialsVersion by mutableStateOf(0)
        private set
    var connected by mutableStateOf(false)
        private set

    @Volatile private var running = false
    @Volatile private var out: DataOutputStream? = null

    fun start() {
        if (running) return
        running = true
        thread(name = "remote-session", isDaemon = true) { connectLoop() }
    }

    fun stop() { running = false }

    /** Injects a touch into the app. [action]: 0 = down, 1 = up, 2 = move; x/y in device pixels. */
    fun sendInput(action: Int, x: Float, y: Float) =
        send(MSG_INPUT, RemoteJson.encodeToString(RemoteInput(action, x, y)).encodeToByteArray())

    /** Sends the workspace copy of [id] to the device, which reloads it. */
    fun pushTutorial(id: String) {
        val json = workspace.readRaw(id) ?: return
        send(MSG_PUSH, RemoteJson.encodeToString(RemoteTutorialFile(id, json)).encodeToByteArray())
    }

    fun deleteTutorialOnDevice(id: String) =
        send(MSG_PUSH, RemoteJson.encodeToString(RemoteTutorialFile(id, null)).encodeToByteArray())

    /** Resets progress for [sectionIds] on the device so the tutorial replays from there. */
    fun previewOnDevice(tutorialId: String, sectionIds: List<String>) =
        send(MSG_COMMAND, RemoteJson.encodeToString(RemoteCommand("preview", tutorialId, sectionIds)).encodeToByteArray())

    private fun send(type: Int, payload: ByteArray) {
        val stream = out ?: return
        try {
            synchronized(stream) {
                stream.writeByte(type)
                stream.writeInt(payload.size)
                stream.write(payload)
                stream.flush()
            }
        } catch (_: Exception) {
            // The read loop notices the dead socket and reconnects.
        }
    }

    private fun connectLoop() {
        while (running) {
            if (!Adb.forward(port)) {
                status = "No device (adb forward failed) - retrying"
                Thread.sleep(2000)
                continue
            }
            try {
                Socket("127.0.0.1", port).use { socket ->
                    socket.tcpNoDelay = true
                    out = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                    status = "Connected"
                    connected = true
                    send(MSG_PULL, ByteArray(0))
                    readLoop(DataInputStream(BufferedInputStream(socket.getInputStream())))
                }
                status = "App disconnected - retrying"
            } catch (_: Exception) {
                status = "Waiting for app (is it running with the editor enabled?)"
            } finally {
                out = null
                connected = false
            }
            Thread.sleep(1500)
        }
    }

    private fun readLoop(input: DataInputStream) {
        while (running) {
            val type = input.readUnsignedByte()
            val payload = ByteArray(input.readInt()).also { input.readFully(it) }
            when (type) {
                MSG_SNAPSHOT -> runCatching { RemoteJson.decodeFromString<RemoteSnapshot>(payload.decodeToString()) }
                    .onSuccess { snapshot = it }
                MSG_TUTORIALS -> runCatching {
                    RemoteJson.decodeFromString<RemoteTutorials>(payload.decodeToString()).tutorials
                        .forEach { f -> f.json?.let { workspace.writeRaw(f.id, it) } }
                    tutorialsVersion++
                }
                MSG_FRAME -> runCatching { Image.makeFromEncoded(payload).toComposeImageBitmap() }
                    .onSuccess { frame = it }
            }
        }
    }
}
