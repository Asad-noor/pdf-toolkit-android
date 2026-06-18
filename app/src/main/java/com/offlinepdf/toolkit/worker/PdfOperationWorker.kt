package com.offlinepdf.toolkit.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig
import com.offlinepdf.toolkit.core.domain.model.WatermarkPosition
import com.offlinepdf.toolkit.core.domain.usecase.pdf.AddWatermarkUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.CompressPdfUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.DeletePagesUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ExtractPagesUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ExtractTextUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ImagesToPdfUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.MergePdfsUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.PasswordProtectUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ReorderPagesUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.RotatePageUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.SplitPdfUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.UnlockPdfUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json

@HiltWorker
class PdfOperationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mergePdfsUseCase: MergePdfsUseCase,
    private val splitPdfUseCase: SplitPdfUseCase,
    private val compressPdfUseCase: CompressPdfUseCase,
    private val rotatePageUseCase: RotatePageUseCase,
    private val extractPagesUseCase: ExtractPagesUseCase,
    private val addWatermarkUseCase: AddWatermarkUseCase,
    private val passwordProtectUseCase: PasswordProtectUseCase,
    private val unlockPdfUseCase: UnlockPdfUseCase,
    private val imagesToPdfUseCase: ImagesToPdfUseCase,
    private val extractTextUseCase: ExtractTextUseCase,
    private val reorderPagesUseCase: ReorderPagesUseCase,
    private val deletePagesUseCase: DeletePagesUseCase,
    private val json: Json
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val operationName = inputData.getString(WorkerKeys.KEY_OPERATION)
            ?: return Result.failure(workDataOf(WorkerKeys.KEY_ERROR to "Missing operation"))
        val operation = runCatching { WorkerKeys.Operation.valueOf(operationName) }.getOrElse {
            return Result.failure(workDataOf(WorkerKeys.KEY_ERROR to "Unknown operation: $operationName"))
        }
        return try {
            when (operation) {
                WorkerKeys.Operation.MERGE -> handleMerge()
                WorkerKeys.Operation.SPLIT -> handleSplit()
                WorkerKeys.Operation.COMPRESS -> handleCompress()
                WorkerKeys.Operation.ROTATE -> handleRotate()
                WorkerKeys.Operation.EXTRACT_PAGES -> handleExtractPages()
                WorkerKeys.Operation.ADD_WATERMARK -> handleAddWatermark()
                WorkerKeys.Operation.PASSWORD_PROTECT -> handlePasswordProtect()
                WorkerKeys.Operation.UNLOCK -> handleUnlock()
                WorkerKeys.Operation.IMAGES_TO_PDF -> handleImagesToPdf()
                WorkerKeys.Operation.EXTRACT_TEXT -> handleExtractText()
                WorkerKeys.Operation.REORDER -> handleReorder()
                WorkerKeys.Operation.DELETE_PAGES -> handleDeletePages()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(workDataOf(WorkerKeys.KEY_ERROR to e.message))
        }
    }

    private suspend fun reportProgress(progress: ProcessingProgress) {
        setProgress(
            workDataOf(
                WorkerKeys.KEY_PROGRESS_CURRENT to progress.current,
                WorkerKeys.KEY_PROGRESS_TOTAL to progress.total,
                WorkerKeys.KEY_PROGRESS_PHASE to progress.phase.name
            )
        )
    }

    private fun uriList(key: String): List<Uri> =
        json.decodeFromString<List<String>>(inputData.getString(key) ?: "[]").map { Uri.parse(it) }

    private fun requiredUri(key: String): Uri =
        Uri.parse(inputData.getString(key) ?: error("Missing $key"))

    private fun optionalPassword(): String? = inputData.getString(WorkerKeys.KEY_PASSWORD)

    private fun pageNumbers(): List<Int> =
        json.decodeFromString(inputData.getString(WorkerKeys.KEY_PAGE_NUMBERS) ?: "[]")

    private suspend fun handleMerge(): Result {
        val inputUris = uriList(WorkerKeys.KEY_INPUT_URIS)
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        mergePdfsUseCase(MergePdfsUseCase.Params(inputUris, outputUri)).collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleSplit(): Result {
        val inputUri = requiredUri(WorkerKeys.KEY_INPUT_URIS).let {
            uriList(WorkerKeys.KEY_INPUT_URIS).first()
        }
        val outputDirUri = requiredUri(WorkerKeys.KEY_OUTPUT_DIR_URI)
        val modeJson = inputData.getString(WorkerKeys.KEY_SPLIT_MODE) ?: error("Missing split mode")
        val mode = json.decodeFromString<SplitModeDto>(modeJson).toDomain()
        val password = optionalPassword()
        splitPdfUseCase(SplitPdfUseCase.Params(inputUri, mode, outputDirUri, password))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleCompress(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        val level = CompressionLevel.valueOf(
            inputData.getString(WorkerKeys.KEY_COMPRESSION_LEVEL) ?: CompressionLevel.MEDIUM.name
        )
        compressPdfUseCase(CompressPdfUseCase.Params(inputUri, outputUri, level, optionalPassword()))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleRotate(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        val rotation = RotationDegree.valueOf(
            inputData.getString(WorkerKeys.KEY_ROTATION) ?: error("Missing rotation")
        )
        val pages = pageNumbers().takeIf { it.isNotEmpty() }
        rotatePageUseCase(RotatePageUseCase.Params(inputUri, outputUri, pages, rotation, optionalPassword()))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleExtractPages(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        extractPagesUseCase(ExtractPagesUseCase.Params(inputUri, outputUri, pageNumbers(), optionalPassword()))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleAddWatermark(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        val configJson = inputData.getString(WorkerKeys.KEY_WATERMARK_CONFIG) ?: error("Missing watermark config")
        val config = json.decodeFromString<WatermarkConfigDto>(configJson).toDomain()
        addWatermarkUseCase(AddWatermarkUseCase.Params(inputUri, outputUri, config, optionalPassword()))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handlePasswordProtect(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        val configJson = inputData.getString(WorkerKeys.KEY_PASSWORD_CONFIG) ?: error("Missing password config")
        val config = json.decodeFromString<PasswordConfigDto>(configJson).toDomain()
        passwordProtectUseCase(PasswordProtectUseCase.Params(inputUri, outputUri, config))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleUnlock(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        val password = optionalPassword() ?: error("Missing password for unlock")
        unlockPdfUseCase(UnlockPdfUseCase.Params(inputUri, outputUri, password))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleImagesToPdf(): Result {
        val imageUris = uriList(WorkerKeys.KEY_INPUT_URIS)
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        imagesToPdfUseCase(ImagesToPdfUseCase.Params(imageUris, outputUri))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleExtractText(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        extractTextUseCase(ExtractTextUseCase.Params(inputUri, optionalPassword()))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleReorder(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        val newOrder: List<Int> = json.decodeFromString(
            inputData.getString(WorkerKeys.KEY_NEW_ORDER) ?: "[]"
        )
        reorderPagesUseCase(ReorderPagesUseCase.Params(inputUri, outputUri, newOrder, optionalPassword()))
            .collect { reportProgress(it) }
        return Result.success()
    }

    private suspend fun handleDeletePages(): Result {
        val inputUri = uriList(WorkerKeys.KEY_INPUT_URIS).first()
        val outputUri = requiredUri(WorkerKeys.KEY_OUTPUT_URI)
        deletePagesUseCase(DeletePagesUseCase.Params(inputUri, outputUri, pageNumbers(), optionalPassword()))
            .collect { reportProgress(it) }
        return Result.success()
    }
}

// ── Serializable DTOs for Worker input data ────────────────────────────────

@kotlinx.serialization.Serializable
data class SplitModeDto(
    val type: String,
    val ranges: List<IntRangeDto>? = null,
    val n: Int? = null,
    val pageNumbers: List<Int>? = null
) {
    fun toDomain(): SplitMode = when (type) {
        "ByRange" -> SplitMode.ByRange(ranges!!.map { it.first..it.last })
        "EveryN" -> SplitMode.EveryN(n!!)
        "AtPages" -> SplitMode.AtPages(pageNumbers!!)
        else -> error("Unknown SplitMode type: $type")
    }
}

@kotlinx.serialization.Serializable
data class IntRangeDto(val first: Int, val last: Int)

@kotlinx.serialization.Serializable
data class WatermarkConfigDto(
    val type: String,
    val text: String? = null,
    val fontSizePt: Float = 36f,
    val opacity: Float = 0.3f,
    val color: Long = 0xFF808080,
    val rotation: Float = 45f,
    val position: String = "CENTER",
    val imageUri: String? = null,
    val scaleFactor: Float = 0.5f
) {
    fun toDomain(): WatermarkConfig {
        val pos = WatermarkPosition.valueOf(position)
        return when (type) {
            "Text" -> WatermarkConfig.TextWatermark(text!!, fontSizePt, opacity, color, rotation, pos)
            "Image" -> WatermarkConfig.ImageWatermark(imageUri!!, opacity, scaleFactor, pos)
            else -> error("Unknown WatermarkConfig type: $type")
        }
    }
}

@kotlinx.serialization.Serializable
data class PasswordConfigDto(
    val userPassword: String,
    val ownerPassword: String,
    val allowPrinting: Boolean = true,
    val allowCopying: Boolean = false
) {
    fun toDomain() = PasswordConfig(userPassword, ownerPassword, allowPrinting, allowCopying)
}
