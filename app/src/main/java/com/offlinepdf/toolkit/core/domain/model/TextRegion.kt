package com.offlinepdf.toolkit.core.domain.model

import android.graphics.RectF

data class TextRegion(
    val pageIndex: Int,
    val text: String,
    val bounds: RectF,       // PDF coordinate space (origin bottom-left, y up)
    val fontName: String,
    val fontSize: Float,     // in PDF points
    val isBold: Boolean,
    val isItalic: Boolean
)
