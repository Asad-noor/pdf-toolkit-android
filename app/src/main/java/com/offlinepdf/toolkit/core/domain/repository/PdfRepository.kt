package com.offlinepdf.toolkit.core.domain.repository

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.PdfDocument
import com.offlinepdf.toolkit.core.domain.model.PdfOperationResult
import com.offlinepdf.toolkit.core.domain.model.PdfPage
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig
import kotlinx.coroutines.flow.Flow

interface PdfRepository {
    suspend fun getDocumentInfo(uri: Uri, password: String? = null): Result<PdfDocument>
    suspend fun getPageList(uri: Uri, password: String? = null): Result<List<PdfPage>>
    fun renderPage(uri: Uri, pageNumber: Int, dpi: Int = 150, password: String? = null): Flow<PdfOperationResult>
    fun mergePdfs(inputUris: List<Uri>, outputUri: Uri): Flow<ProcessingProgress>
    fun splitPdf(inputUri: Uri, mode: SplitMode, outputDir: Uri, password: String? = null): Flow<ProcessingProgress>
    fun compressPdf(inputUri: Uri, outputUri: Uri, level: CompressionLevel, password: String? = null): Flow<ProcessingProgress>
    fun rotatePages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>?, rotation: RotationDegree, password: String? = null): Flow<ProcessingProgress>
    fun extractPages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>, password: String? = null): Flow<ProcessingProgress>
    fun addWatermark(inputUri: Uri, outputUri: Uri, config: WatermarkConfig, password: String? = null): Flow<ProcessingProgress>
    fun passwordProtect(inputUri: Uri, outputUri: Uri, config: PasswordConfig): Flow<ProcessingProgress>
    fun unlockPdf(inputUri: Uri, outputUri: Uri, currentPassword: String): Flow<ProcessingProgress>
    fun imagesToPdf(imageUris: List<Uri>, outputUri: Uri): Flow<ProcessingProgress>
    fun extractText(inputUri: Uri, password: String? = null): Flow<ProcessingProgress>
    fun reorderPages(inputUri: Uri, outputUri: Uri, newOrder: List<Int>, password: String? = null): Flow<ProcessingProgress>
    fun deletePages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>, password: String? = null): Flow<ProcessingProgress>
}
