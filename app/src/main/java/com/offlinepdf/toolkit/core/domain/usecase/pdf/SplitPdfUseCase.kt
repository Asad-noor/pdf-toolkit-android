package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SplitPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<SplitPdfUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val mode: SplitMode,
        val outputDirUri: Uri,
        val password: String? = null
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.splitPdf(params.inputUri, params.mode, params.outputDirUri, params.password)
}
