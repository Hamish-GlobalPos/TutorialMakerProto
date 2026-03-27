package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
data class TargetSpec(
    val tag: String? = null,
    // Spotlight position as fractions of viewport (0.0–1.0), used in SCROLL mode.
    val fallbackXFrac: Float? = null,
    val fallbackYFrac: Float? = null,
    val fallbackWidthFrac: Float? = null,
    val fallbackHeightFrac: Float? = null,
    val scrollContainerTag: String? = null
)
