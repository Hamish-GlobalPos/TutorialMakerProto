package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
data class TutorialSection(
    val id: String,
    val title: String = "",
    val steps: List<TutorialStep> = emptyList()
)
