package com.pano.tutorialmaker.io

import com.pano.tutorialmaker.model.Tutorial
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

class TutorialFileManager(
    private val basePath: String,
    private val fileSystem: FileSystem = systemFileSystem
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val tutorialsDir get() = "$basePath/tutorials".toPath()

    fun saveTutorial(tutorial: Tutorial) {
        fileSystem.createDirectories(tutorialsDir)
        val filePath = tutorialsDir / "${tutorial.id}.json"
        val content = json.encodeToString(tutorial)
        fileSystem.write(filePath) {
            writeUtf8(content)
        }
    }

    fun loadTutorial(id: String): Tutorial? {
        val filePath = tutorialsDir / "$id.json"
        if (!fileSystem.exists(filePath)) return null
        val content = fileSystem.read(filePath) {
            readUtf8()
        }
        return json.decodeFromString<Tutorial>(content)
    }

    /** The raw JSON text of a saved tutorial, for syncing files between devices untouched. */
    fun readRaw(id: String): String? {
        val filePath = tutorialsDir / "$id.json"
        if (!fileSystem.exists(filePath)) return null
        return fileSystem.read(filePath) { readUtf8() }
    }

    fun writeRaw(id: String, content: String) {
        fileSystem.createDirectories(tutorialsDir)
        fileSystem.write(tutorialsDir / "$id.json") { writeUtf8(content) }
    }

    fun listTutorials(): List<String> {
        if (!fileSystem.exists(tutorialsDir)) return emptyList()
        return fileSystem.list(tutorialsDir)
            .filter { it.name.endsWith(".json") }
            .map { it.name.removeSuffix(".json") }
    }

    fun deleteTutorial(id: String): Boolean {
        val filePath = tutorialsDir / "$id.json"
        if (!fileSystem.exists(filePath)) return false
        fileSystem.delete(filePath)
        return true
    }
}
