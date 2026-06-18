package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AddWatermarkUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<AddWatermarkUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val outputUri: Uri,
        val config: WatermarkConfig,
        val password: String? = null
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.addWatermark(params.inputUri, params.outputUri, params.config, params.password)
}
