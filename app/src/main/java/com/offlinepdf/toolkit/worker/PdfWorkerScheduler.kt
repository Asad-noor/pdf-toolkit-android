package com.offlinepdf.toolkit.worker

import android.net.Uri
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfWorkerScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val json: Json
) {
    private val storageConstraints = Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .build()

    fun enqueueMerge(inputUris: List<Uri>, outputUri: Uri): UUID =
        enqueue(
            WorkerKeys.Operation.MERGE,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(inputUris.map { it.toString() }),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString()
        )

    fun enqueueSplit(inputUri: Uri, mode: SplitMode, outputDirUri: Uri, password: String? = null): UUID {
        val modeDto = when (mode) {
            is SplitMode.ByRange -> SplitModeDto("ByRange", ranges = mode.ranges.map { IntRangeDto(it.first, it.last) })
            is SplitMode.EveryN -> SplitModeDto("EveryN", n = mode.n)
            is SplitMode.AtPages -> SplitModeDto("AtPages", pageNumbers = mode.pageNumbers)
        }
        return enqueue(
            WorkerKeys.Operation.SPLIT,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_DIR_URI to outputDirUri.toString(),
            WorkerKeys.KEY_SPLIT_MODE to json.encodeToString(modeDto),
            WorkerKeys.KEY_PASSWORD to password
        )
    }

    fun enqueueCompress(inputUri: Uri, outputUri: Uri, level: CompressionLevel, password: String? = null): UUID =
        enqueue(
            WorkerKeys.Operation.COMPRESS,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_COMPRESSION_LEVEL to level.name,
            WorkerKeys.KEY_PASSWORD to password
        )

    fun enqueueRotate(inputUri: Uri, outputUri: Uri, rotation: RotationDegree, pageNumbers: List<Int>?, password: String? = null): UUID =
        enqueue(
            WorkerKeys.Operation.ROTATE,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_ROTATION to rotation.name,
            WorkerKeys.KEY_PAGE_NUMBERS to json.encodeToString<List<Int>>(pageNumbers ?: emptyList()),
            WorkerKeys.KEY_PASSWORD to password
        )

    fun enqueueExtractPages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>, password: String? = null): UUID =
        enqueue(
            WorkerKeys.Operation.EXTRACT_PAGES,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_PAGE_NUMBERS to json.encodeToString<List<Int>>(pageNumbers),
            WorkerKeys.KEY_PASSWORD to password
        )

    fun enqueueAddWatermark(inputUri: Uri, outputUri: Uri, config: WatermarkConfig, password: String? = null): UUID {
        val dto = when (config) {
            is WatermarkConfig.TextWatermark -> WatermarkConfigDto(
                type = "Text", text = config.text, fontSizePt = config.fontSizePt,
                opacity = config.opacity, color = config.color, rotation = config.rotation,
                position = config.position.name
            )
            is WatermarkConfig.ImageWatermark -> WatermarkConfigDto(
                type = "Image", imageUri = config.imageUri, opacity = config.opacity,
                scaleFactor = config.scaleFactor, position = config.position.name
            )
        }
        return enqueue(
            WorkerKeys.Operation.ADD_WATERMARK,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_WATERMARK_CONFIG to json.encodeToString(dto),
            WorkerKeys.KEY_PASSWORD to password
        )
    }

    fun enqueuePasswordProtect(inputUri: Uri, outputUri: Uri, config: PasswordConfig): UUID {
        val dto = PasswordConfigDto(config.userPassword, config.ownerPassword, config.allowPrinting, config.allowCopying)
        return enqueue(
            WorkerKeys.Operation.PASSWORD_PROTECT,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_PASSWORD_CONFIG to json.encodeToString(dto)
        )
    }

    fun enqueueUnlock(inputUri: Uri, outputUri: Uri, currentPassword: String): UUID =
        enqueue(
            WorkerKeys.Operation.UNLOCK,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_PASSWORD to currentPassword
        )

    fun enqueueImagesToPdf(imageUris: List<Uri>, outputUri: Uri): UUID =
        enqueue(
            WorkerKeys.Operation.IMAGES_TO_PDF,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(imageUris.map { it.toString() }),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString()
        )

    fun enqueueExtractText(inputUri: Uri, password: String? = null): UUID =
        enqueue(
            WorkerKeys.Operation.EXTRACT_TEXT,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_PASSWORD to password
        )

    fun enqueueReorder(inputUri: Uri, outputUri: Uri, newOrder: List<Int>, password: String? = null): UUID =
        enqueue(
            WorkerKeys.Operation.REORDER,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_NEW_ORDER to json.encodeToString<List<Int>>(newOrder),
            WorkerKeys.KEY_PASSWORD to password
        )

    fun enqueueDeletePages(inputUri: Uri, outputUri: Uri, pageNumbers: List<Int>, password: String? = null): UUID =
        enqueue(
            WorkerKeys.Operation.DELETE_PAGES,
            WorkerKeys.KEY_INPUT_URIS to json.encodeToString<List<String>>(listOf(inputUri.toString())),
            WorkerKeys.KEY_OUTPUT_URI to outputUri.toString(),
            WorkerKeys.KEY_PAGE_NUMBERS to json.encodeToString<List<Int>>(pageNumbers),
            WorkerKeys.KEY_PASSWORD to password
        )

    fun observeWork(workId: UUID): Flow<WorkInfo> =
        workManager.getWorkInfoByIdFlow(workId).filterNotNull()

    private fun enqueue(operation: WorkerKeys.Operation, vararg pairs: Pair<String, Any?>): UUID {
        val filteredPairs = pairs.filter { it.second != null }
            .map { it.first to it.second.toString() }
            .toTypedArray()
        val request = OneTimeWorkRequestBuilder<PdfOperationWorker>()
            .setInputData(workDataOf(
                WorkerKeys.KEY_OPERATION to operation.name,
                *filteredPairs
            ))
            .setConstraints(storageConstraints)
            .build()
        workManager.enqueue(request)
        return request.id
    }
}
