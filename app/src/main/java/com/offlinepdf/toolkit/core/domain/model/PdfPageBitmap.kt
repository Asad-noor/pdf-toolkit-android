package com.offlinepdf.toolkit.core.domain.model

import android.graphics.Bitmap

data class PdfPageBitmap(
    val pageNumber: Int,
    val bitmap: Bitmap,
    val renderDpi: Int
)
