package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CompressPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<CompressPdfUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val outputUri: Uri,
        val level: CompressionLevel,
        val password: String? = null
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.compressPdf(params.inputUri, params.outputUri, params.level, params.password)
}
