package com.offlinepdf.toolkit.core.domain.model

data class PdfDocument(
    val uri: String,
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val isPasswordProtected: Boolean,
    val author: String? = null,
    val title: String? = null,
    val creationDate: String? = null
)
