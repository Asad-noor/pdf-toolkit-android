package com.offlinepdf.toolkit.core.domain.model

data class PdfPage(
    val pageNumber: Int,
    val widthPt: Float,
    val heightPt: Float,
    val rotation: Int
)
