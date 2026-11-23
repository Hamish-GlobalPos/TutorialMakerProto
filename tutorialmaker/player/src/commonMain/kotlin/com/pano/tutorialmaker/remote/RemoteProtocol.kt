package com.pano.tutorialmaker.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val REMOTE_EDITOR_DEFAULT_PORT = 8765

/*
 * Wire format, both directions: [type: 1 byte][length: 4 bytes big-endian][payload].
 */

/** Device -> PC. Payload: [RemoteSnapshot] as UTF-8 JSON. Sent on connect and on every change. */
const val MSG_SNAPSHOT = 1

/** Device -> PC. Payload: a JPEG of the app window. */
const val MSG_FRAME = 2

/** PC -> Device. Payload: [RemoteInput] as UTF-8 JSON. */
const val MSG_INPUT = 3

/** PC -> Device. Empty payload: asks the device to send all its tutorials ([MSG_TUTORIALS]). */
const val MSG_PULL = 4

/** Device -> PC. Payload: [RemoteTutorials] as UTF-8 JSON. */
const val MSG_TUTORIALS = 5

/** PC -> Device. Payload: [RemoteTutorialFile] as UTF-8 JSON. A null [RemoteTutorialFile.json] deletes. */
const val MSG_PUSH = 6

/** PC -> Device. Payload: [RemoteCommand] as UTF-8 JSON. */
const val MSG_COMMAND = 7

/** A tagged element's bounds, in the app's root coordinate space (pixels). */
@Serializable
data class RemoteTag(
    val tag: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class RemoteSnapshot(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val tags: List<RemoteTag>,
    val screens: List<String>
)

/**
 * A touch to inject into the app. [action] uses Android's MotionEvent values:
 * 0 = down, 1 = up, 2 = move. [x]/[y] are in the app's root pixels (same space as tag bounds).
 */
@Serializable
data class RemoteInput(val action: Int, val x: Float, val y: Float)

/** One tutorial file as stored on disk. [json] is the raw file content; null means "deleted". */
@Serializable
data class RemoteTutorialFile(val id: String, val json: String? = null)

@Serializable
data class RemoteTutorials(val tutorials: List<RemoteTutorialFile>)

/**
 * A command for the device. Currently only "preview": reset progress for [sectionIds] of
 * [tutorialId] and reload, so the tutorial replays as the user navigates.
 */
@Serializable
data class RemoteCommand(
    val cmd: String,
    val tutorialId: String? = null,
    val sectionIds: List<String> = emptyList()
)

val RemoteJson = Json { ignoreUnknownKeys = true }
