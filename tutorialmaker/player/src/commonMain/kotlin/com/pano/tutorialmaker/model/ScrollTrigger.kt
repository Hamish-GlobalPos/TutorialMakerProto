package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
data class ScrollTrigger(
    val yFraction: Float? = null,  // horizontal line; 0.0=top, 1.0=bottom of viewport
    val xFraction: Float? = null   // vertical line;   0.0=left, 1.0=right of viewport
)
