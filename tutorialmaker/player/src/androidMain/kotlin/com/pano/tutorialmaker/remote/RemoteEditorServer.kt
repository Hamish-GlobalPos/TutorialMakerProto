package com.pano.tutorialmaker.remote

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

actual class RemoteEditorServer actual constructor(private val port: Int) {
    private class Client(val socket: Socket) {
        val out = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
    }

    private var serverSocket: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<Client>()
    @Volatile private var lastSnapshot: ByteArray? = null

    // publish() is called from the UI thread; sockets must be written off it.
    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "tutorial-remote-writer").apply { isDaemon = true } }
    private val pendingFrames = AtomicInteger(0)

    private val listeners = CopyOnWriteArrayList<(Int, ByteArray) -> Unit>()
    actual fun addListener(listener: (type: Int, payload: ByteArray) -> Unit) { listeners.add(listener) }
    actual fun removeListener(listener: (type: Int, payload: ByteArray) -> Unit) { listeners.remove(listener) }
    actual val hasClients: Boolean get() = clients.isNotEmpty()

    // Requires android.permission.INTERNET in the host app's manifest (debug builds).
    actual fun start() {
        if (serverSocket != null) return
        // Loopback only: adb forward reaches it, other devices on the network cannot.
        val ss = runCatching { ServerSocket(port, 4, InetAddress.getByName("127.0.0.1")) }.getOrNull() ?: return
        serverSocket = ss
        Thread({
            while (!ss.isClosed) {
                val socket = try { ss.accept() } catch (_: Exception) { break }
                socket.tcpNoDelay = true
                val client = Client(socket)
                clients.add(client)
                lastSnapshot?.let { write(client, MSG_SNAPSHOT, it) }
                Thread({ readLoop(client) }, "tutorial-remote-reader").apply { isDaemon = true }.start()
            }
        }, "tutorial-remote-editor").apply { isDaemon = true }.start()
    }

    actual fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        clients.forEach { runCatching { it.socket.close() } }
        clients.clear()
    }

    actual fun publish(type: Int, payload: ByteArray) {
        if (type == MSG_SNAPSHOT) lastSnapshot = payload
        if (clients.isEmpty()) return
        if (type == MSG_FRAME) {
            // Slow link: drop frames rather than queueing a growing backlog.
            if (pendingFrames.incrementAndGet() > 2) { pendingFrames.decrementAndGet(); return }
        }
        writer.execute {
            try { clients.forEach { write(it, type, payload) } }
            finally { if (type == MSG_FRAME) pendingFrames.decrementAndGet() }
        }
    }

    private fun write(client: Client, type: Int, payload: ByteArray) {
        try {
            synchronized(client.out) {
                client.out.writeByte(type)
                client.out.writeInt(payload.size)
                client.out.write(payload)
                client.out.flush()
            }
        } catch (_: Exception) {
            drop(client)
        }
    }

    private fun readLoop(client: Client) {
        try {
            val input = DataInputStream(BufferedInputStream(client.socket.getInputStream()))
            while (true) {
                val type = input.readUnsignedByte()
                val payload = ByteArray(input.readInt()).also { input.readFully(it) }
                listeners.forEach { runCatching { it(type, payload) } }
            }
        } catch (_: Exception) {
            drop(client)
        }
    }

    private fun drop(client: Client) {
        clients.remove(client)
        runCatching { client.socket.close() }
    }
}
