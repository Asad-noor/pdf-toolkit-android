package com.offlinepdf.toolkit.core.domain.error

sealed class PdfError {
    data class FileNotFound(val uri: String) : PdfError()
    data class FileReadPermissionDenied(val uri: String) : PdfError()
    data class FileWritePermissionDenied(val uri: String) : PdfError()
    data object StorageFull : PdfError()

    data class PasswordRequired(val uri: String) : PdfError()
    data class WrongPassword(val uri: String) : PdfError()
    data class CorruptedPdf(val uri: String, val detail: String? = null) : PdfError()
    data object EmptyDocument : PdfError()
    data class PageOutOfBounds(val requested: Int, val max: Int) : PdfError()
    data class InvalidPageRange(val range: String) : PdfError()

    data class MergeFailure(val reason: String) : PdfError()
    data class SplitFailure(val reason: String) : PdfError()
    data class CompressionFailure(val reason: String) : PdfError()
    data class WatermarkFailure(val reason: String) : PdfError()
    data class EncryptionFailure(val reason: String) : PdfError()
    data class RenderFailure(val page: Int, val reason: String) : PdfError()
    data class TextExtractionFailure(val reason: String) : PdfError()

    data class OutOfMemory(val operation: String) : PdfError()
    data class OperationCancelled(val operation: String) : PdfError()
    data class UnknownError(val throwable: Throwable) : PdfError()
}
