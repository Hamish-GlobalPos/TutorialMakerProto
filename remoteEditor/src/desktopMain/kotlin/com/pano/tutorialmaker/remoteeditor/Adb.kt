package com.pano.tutorialmaker.remoteeditor

import java.io.File
import java.util.concurrent.TimeUnit

/** Only used to set up the port forward; frames and input travel over the socket itself. */
object Adb {
    private val exe: String by lazy { locate() }

    private fun locate(): String {
        val name = if (System.getProperty("os.name").contains("win", true)) "adb.exe" else "adb"
        val fromLocalProps = File("local.properties").takeIf { it.exists() }?.readLines()
            ?.firstOrNull { it.startsWith("sdk.dir=") }
            ?.removePrefix("sdk.dir=")
            ?.replace("\\\\", "\\")
            ?.replace("\\:", ":")
        val roots = listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            fromLocalProps,
            System.getenv("LOCALAPPDATA")?.let { "$it/Android/Sdk" }
        )
        return roots.map { File(it, "platform-tools/$name") }.firstOrNull { it.exists() }?.path ?: "adb"
    }

    fun forward(port: Int): Boolean = runCatching {
        val p = ProcessBuilder(exe, "forward", "tcp:$port", "tcp:$port")
            .redirectErrorStream(true).start()
        p.inputStream.readBytes()
        p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0
    }.getOrDefault(false)
}
