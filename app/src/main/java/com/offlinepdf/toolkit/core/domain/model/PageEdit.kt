package com.offlinepdf.toolkit.core.domain.model

import android.graphics.Bitmap

data class PageEdit(
    val pageIndex: Int,
    val overlayBitmap: Bitmap?   // transparent ARGB_8888 bitmap; null means no edits
)
