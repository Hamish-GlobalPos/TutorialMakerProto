package com.pano.tutorialmaker.io

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

class TutorialProgressManager(
    private val basePath: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val progressFile get() = "$basePath/tutorial_progress.json".toPath()

    private var cache: MutableMap<String, MutableSet<String>>? = null

    private fun load(): MutableMap<String, MutableSet<String>> {
        cache?.let { return it }
        val data = if (fileSystem.exists(progressFile)) {
            val content = fileSystem.read(progressFile) { readUtf8() }
            try {
                val raw = json.decodeFromString<Map<String, List<String>>>(content)
                raw.mapValues { it.value.toMutableSet() }.toMutableMap()
            } catch (_: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
        cache = data
        return data
    }

    private fun save() {
        val data = cache ?: return
        val serializable = data.mapValues { it.value.toList() }
        val dir = progressFile.parent
        if (dir != null) fileSystem.createDirectories(dir)
        fileSystem.write(progressFile) {
            writeUtf8(json.encodeToString(serializable))
        }
    }

    fun isSectionCompleted(tutorialId: String, sectionId: String): Boolean {
        return load()[tutorialId]?.contains(sectionId) == true
    }

    fun markSectionCompleted(tutorialId: String, sectionId: String) {
        val data = load()
        data.getOrPut(tutorialId) { mutableSetOf() }.add(sectionId)
        save()
    }

    fun reset(tutorialId: String) {
        val data = load()
        data.remove(tutorialId)
        save()
    }

    fun resetSection(tutorialId: String, sectionId: String) {
        val data = load()
        data[tutorialId]?.remove(sectionId)
        save()
    }

    fun resetAll() {
        cache = mutableMapOf()
        save()
    }
}
