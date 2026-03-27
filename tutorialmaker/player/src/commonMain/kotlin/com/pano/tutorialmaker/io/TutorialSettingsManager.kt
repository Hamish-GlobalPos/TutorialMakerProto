package com.pano.tutorialmaker.io

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

@Serializable
private data class TutorialSettings(
    val playOnEveryStart: Boolean = false
)

class TutorialSettingsManager(
    private val basePath: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val settingsFile get() = "$basePath/tutorial_settings.json".toPath()

    private var cache: TutorialSettings? = null

    private fun load(): TutorialSettings {
        cache?.let { return it }
        val settings = if (fileSystem.exists(settingsFile)) {
            val content = fileSystem.read(settingsFile) { readUtf8() }
            try { json.decodeFromString<TutorialSettings>(content) } catch (_: Exception) { TutorialSettings() }
        } else {
            TutorialSettings()
        }
        cache = settings
        return settings
    }

    private fun save(settings: TutorialSettings) {
        cache = settings
        val dir = settingsFile.parent
        if (dir != null) fileSystem.createDirectories(dir)
        fileSystem.write(settingsFile) { writeUtf8(json.encodeToString(settings)) }
    }

    fun getPlayOnEveryStart(): Boolean = load().playOnEveryStart

    fun setPlayOnEveryStart(value: Boolean) {
        save(load().copy(playOnEveryStart = value))
    }
}
