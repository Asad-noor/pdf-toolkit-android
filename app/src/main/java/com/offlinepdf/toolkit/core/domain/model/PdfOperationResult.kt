package com.offlinepdf.toolkit.core.domain.model

import com.offlinepdf.toolkit.core.domain.error.PdfError

sealed class PdfOperationResult {
    data class Success(val outputUri: String, val outputFileName: String) : PdfOperationResult()
    data class SuccessWithText(val text: String) : PdfOperationResult()
    data class SuccessWithMultiple(val outputs: List<Pair<String, String>>) : PdfOperationResult()
    data class Failure(val error: PdfError) : PdfOperationResult()
}
