package com.pano.tutorialmaker.model

import kotlinx.serialization.Serializable

@Serializable
enum class TextPosition {
    ABOVE,
    BELOW,
    LEFT,
    RIGHT,
    CENTER
}
