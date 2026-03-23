package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
data class Tutorial(
    val id: String,
    val name: String = "",
    val description: String = "",
    val version: Int = 1,
    val sections: List<TutorialSection> = emptyList()
)
