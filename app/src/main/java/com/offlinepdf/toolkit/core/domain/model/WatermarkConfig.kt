package com.offlinepdf.toolkit.core.domain.model

sealed class WatermarkConfig {
    data class TextWatermark(
        val text: String,
        val fontSizePt: Float = 36f,
        val opacity: Float = 0.3f,
        val color: Long = 0xFF808080,
        val rotation: Float = 45f,
        val position: WatermarkPosition = WatermarkPosition.CENTER
    ) : WatermarkConfig()

    data class ImageWatermark(
        val imageUri: String,
        val opacity: Float = 0.3f,
        val scaleFactor: Float = 0.5f,
        val position: WatermarkPosition = WatermarkPosition.CENTER
    ) : WatermarkConfig()
}

enum class WatermarkPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}
