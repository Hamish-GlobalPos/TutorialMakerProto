package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
data class TargetSpec(
    val tag: String? = null,
    val fallbackXDp: Float? = null,
    val fallbackYDp: Float? = null,
    val fallbackWidthDp: Float? = null,
    val fallbackHeightDp: Float? = null
)
