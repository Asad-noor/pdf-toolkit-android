package com.offlinepdf.toolkit.core.data.repository

import android.content.Context
import android.net.Uri
import com.offlinepdf.toolkit.core.data.processor.AndroidPdfRenderer
import com.offlinepdf.toolkit.core.data.processor.ITextPdfProcessor
import com.offlinepdf.toolkit.core.data.processor.PdfProcessingException
import com.offlinepdf.toolkit.core.domain.error.PdfError
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.PdfDocument
import com.offlinepdf.toolkit.core.domain.model.PdfOperationResult
import com.offlinepdf.toolkit.core.domain.model.PdfPage
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig
import com.offlinepdf.toolkit.core.domain.repository.FileRepository
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processor: ITextPdfProcessor,
    private val renderer: AndroidPdfRenderer,
    private val fileRepository: FileRepository
) : PdfRepository {

    override suspend fun getDocumentInfo(uri: Uri, password: String?): Result<PdfDocument> = runCatching {
        val bytes = fileRepository.openInputStream(uri).getOrThrow().use { it.readBytes() }
        val fileName = fileRepository.getFileName(uri).getOrElse { "unknown.pdf" }
        val fileSize = fileRepository.getFileSize(uri).getOrElse { bytes.size.toLong() }
        val pageCount = processor.getPageCount(bytes, password)
        val meta = processor.getMetadata(bytes, password)
        val isProtected = processor.isPasswordProtected(bytes)
        PdfDocument(
            uri = uri.toString(),
            fileName = fileName,
            pageCount = pageCount,
            fileSizeBytes = fileSize,
            isPasswordProtected = isProtected,
            author = meta["author"],
            title = meta["title"],
            creationDate = meta["creationDate"]
        )
    }.mapFailure(uri.toString())

    override suspend fun getPageList(uri: Uri, password: String?): Result<List<PdfPage>> = runCatching {
        val bytes = fileRepository.openInputStream(uri).getOrThrow().use { it.readBytes() }
        val pageCount = processor.getPageCount(bytes, password)
        (1..pageCount).map { pageNum ->
            val (w, h) = processor.getPageDimensions(bytes, password, pageNum)
            val rotation = processor.getRotation(bytes, password, pageNum)
            PdfPage(pageNumber = pageNum - 1, widthPt = w, heightPt = h, rotation = rotation)
        }
    }.mapFailure(uri.toString())

    override fun renderPage(uri: Uri, pageNumber: Int, dpi: Int, password: String?): Flow<PdfOperationResult> = flow {
        val result = renderer.renderPage(uri, pageNumber, dpi)
        if (result.isSuccess) {
            emit(PdfOperationResult.Success(uri.toString(), "page_$pageNumber"))
        } else {
            emit(PdfOperationResult.Failure(PdfError.RenderFailure(pageNumber, result.exceptionOrNull()?.message ?: "Unknown")))
        }
    }.flowOn(Dispatchers.IO)

    override fun mergePdfs(inputUris: List<Uri>, outputUri: Uri): Flow<ProcessingProgress> = flow {
        emit(ProcessingProgress(0, inputUris.size, ProcessingProgress.Phase.READING))
        val inputBytes = inputUris.mapIndexed { index, uri ->
            fileRepository.openInputStream(uri).getOrThrow().use { it.readBytes() }.also {
                emit(ProcessingProgress(index + 1, inputUris.size, ProcessingProgress.Phase.READING))
            }
        }
        emit(ProcessingProgress(0, inputUris.size, ProcessingProgress.Phase.PROCESSING))
        val outputStream = fileRepository.openOutputStream(outputUri).getOrThrow()
        outputStream.use {
            processor.merge(inputBytes, it) { current, total ->
                // progress emitted via callbackFlow not possible in plain flow — best effort
            }
        }
        emit(ProcessingProgress(inputUris.size, inputUris.size, ProcessingProgress.Phase.DONE))
    }.flowOn(Dispatchers.IO)

    override fun splitPdf(inputUri: Uri, mode: SplitMode, outputDir: Uri, password: String?): Flow<ProcessingProgress> = flow {
        emit(ProcessingProgress(0, 1, ProcessingProgress.Phase.READING))
        val bytes = fileRepository.openInputStream(inputUri).getOrThrow().use { it.readBytes() }
        val baseName = fileRepository.getFileName(inputUri).getOrElse { "split" }.removeSuffix(".pdf")

        var chunkCount = 0
        emit(ProcessingProgress(0, 1, ProcessingProgress.Phase.PROCESSING))
        val chunks = mutableListOf<Pair<Int, ByteArray>>()
        processor.split(
            input = bytes,
            mode = mode,
            password = password,
            onChunk = { index, chunkBytes -> chunks.add(index to chunkBytes) },
            onProgress = { _, _ -> }
        )
        for ((index, chunkBytes) in chunks) {
            val chunkUri = fileRepository.createOutputUri(outputDir, "${baseName}_part${index + 1}.pdf", "application/pdf")
                .getOrThrow()
            fileRepository.openOutputStream(chunkUri).getOrThrow().use { it.write(chunkBytes) }
            chunkCount++
        }
        emit(ProcessingProgress(chunkCount, chunkCount, ProcessingProgress.Phase.DONE))
    }.flowOn(Dispatchers.IO)

    override fun compressPdf(inputUri: Uri, outputUri: Uri, level: CompressionLevel, password: String?): Flow<ProcessingProgress> =
        processSingleInOut(inputUri, outputUri, ProcessingProgress.Phase.PROCESSING) { bytes, os, progressCallback ->
            processor.compress(bytes, level, password, os, progressCallback)
        }

    override fun rotatePages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>?, rotation: RotationDegree, password: String?): Flow<ProcessingProgress> =
        processSingleInOut(inputUri, outputUri, ProcessingProgress.Phase.PROCESSING) { bytes, os, progressCallback ->
            processor.rotatePages(bytes, pageNumbers, rotation, password, os, progressCallback)
        }

    override fun extractPages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>, password: String?): Flow<ProcessingProgress> =
        processSingleInOut(inputUri, outputUri, ProcessingProgress.Phase.PROCESSING) { bytes, os, progressCallback ->
            processor.extractPages(bytes, pageNumbers, password, os, progressCallback)
        }

    override fun addWatermark(inputUri: Uri, outputUri: Uri, config: WatermarkConfig, password: String?): Flow<ProcessingProgress> = flow {
        emit(ProcessingProgress(0, 1, ProcessingProgress.Phase.READING))
        val bytes = fileRepository.openInputStream(inputUri).getOrThrow().use { it.readBytes() }
        emit(ProcessingProgress(1, 1, ProcessingProgress.Phase.PROCESSING))
        val os = fileRepository.openOutputStream(outputUri).getOrThrow()
        os.use {
            when (config) {
                is WatermarkConfig.TextWatermark ->
                    processor.addTextWatermark(bytes, config, password, it) { _, _ -> }
                is WatermarkConfig.ImageWatermark -> {
                    val imgBytes = fileRepository.openInputStream(Uri.parse(config.imageUri))
                        .getOrThrow().use { s -> s.readBytes() }
                    processor.addImageWatermark(bytes, config, imgBytes, password, it) { _, _ -> }
                }
            }
        }
        emit(ProcessingProgress(1, 1, ProcessingProgress.Phase.DONE))
    }.flowOn(Dispatchers.IO)

    override fun passwordProtect(inputUri: Uri, outputUri: Uri, config: PasswordConfig): Flow<ProcessingProgress> =
        processSingleInOut(inputUri, outputUri, ProcessingProgress.Phase.PROCESSING) { bytes, os, progressCallback ->
            processor.encrypt(bytes, config, os, progressCallback)
        }

    override fun unlockPdf(inputUri: Uri, outputUri: Uri, currentPassword: String): Flow<ProcessingProgress> =
        processSingleInOut(inputUri, outputUri, ProcessingProgress.Phase.PROCESSING) { bytes, os, progressCallback ->
            processor.unlock(bytes, currentPassword, os, progressCallback)
        }

    override fun imagesToPdf(imageUris: List<Uri>, outputUri: Uri): Flow<ProcessingProgress> = flow {
        emit(ProcessingProgress(0, imageUris.size, ProcessingProgress.Phase.READING))
        val imageBytes = imageUris.mapIndexed { index, uri ->
            fileRepository.openInputStream(uri).getOrThrow().use { it.readBytes() }.also {
                emit(ProcessingProgress(index + 1, imageUris.size, ProcessingProgress.Phase.READING))
            }
        }
        emit(ProcessingProgress(0, imageUris.size, ProcessingProgress.Phase.PROCESSING))
        val os = fileRepository.openOutputStream(outputUri).getOrThrow()
        os.use { processor.imagesToPdf(imageBytes, it) { _, _ -> } }
        emit(ProcessingProgress(imageUris.size, imageUris.size, ProcessingProgress.Phase.DONE))
    }.flowOn(Dispatchers.IO)

    override fun extractText(inputUri: Uri, password: String?): Flow<ProcessingProgress> = flow {
        emit(ProcessingProgress(0, 1, ProcessingProgress.Phase.READING))
        val bytes = fileRepository.openInputStream(inputUri).getOrThrow().use { it.readBytes() }
        emit(ProcessingProgress(0, 1, ProcessingProgress.Phase.PROCESSING))
        val text = processor.extractText(bytes, password) { current, total ->
            // progress internally tracked
        }
        // Emit result as a message in the final DONE progress
        emit(ProcessingProgress(1, 1, ProcessingProgress.Phase.DONE, text))
    }.flowOn(Dispatchers.IO)

    override fun reorderPages(inputUri: Uri, outputUri: Uri, newOrder: List<Int>, password: String?): Flow<ProcessingProgress> =
        processSingleInOut(inputUri, outputUri, ProcessingProgress.Phase.PROCESSING) { bytes, os, progressCallback ->
            processor.reorderPages(bytes, newOrder, password, os, progressCallback)
        }

    override fun deletePages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>, password: String?): Flow<ProcessingProgress> =
        processSingleInOut(inputUri, outputUri, ProcessingProgress.Phase.PROCESSING) { bytes, os, progressCallback ->
            processor.deletePages(bytes, pageNumbers, password, os, progressCallback)
        }

    private fun processSingleInOut(
        inputUri: Uri,
        outputUri: Uri,
        phase: ProcessingProgress.Phase,
        operation: (ByteArray, java.io.OutputStream, (Int, Int) -> Unit) -> Unit
    ): Flow<ProcessingProgress> = flow {
        emit(ProcessingProgress(0, 1, ProcessingProgress.Phase.READING))
        val bytes = fileRepository.openInputStream(inputUri).getOrThrow().use { it.readBytes() }
        emit(ProcessingProgress(0, 1, phase))
        val os = fileRepository.openOutputStream(outputUri).getOrThrow()
        os.use { operation(bytes, it) { _, _ -> } }
        emit(ProcessingProgress(1, 1, ProcessingProgress.Phase.DONE))
    }.flowOn(Dispatchers.IO)

    private fun <T> Result<T>.mapFailure(uri: String): Result<T> = this.recoverCatching { e ->
        throw when {
            e.message?.contains("password", ignoreCase = true) == true ->
                PdfProcessingException(PdfError.PasswordRequired(uri), e)
            e is OutOfMemoryError ->
                PdfProcessingException(PdfError.OutOfMemory("getDocumentInfo"), e)
            else ->
                PdfProcessingException(PdfError.UnknownError(e), e)
        }
    }
}
