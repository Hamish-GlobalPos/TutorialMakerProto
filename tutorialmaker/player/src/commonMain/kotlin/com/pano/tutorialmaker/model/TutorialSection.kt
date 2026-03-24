package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
enum class TriggerType {
    SCREEN,
    ELEMENT
}

@Serializable
data class TutorialSection(
    val id: String,
    val title: String = "",
    val triggerType: TriggerType = TriggerType.SCREEN,
    val screenTag: String? = null,
    val elementTag: String? = null,
    val steps: List<TutorialStep> = emptyList()
)
