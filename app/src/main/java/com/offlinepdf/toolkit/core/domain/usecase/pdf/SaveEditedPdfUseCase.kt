package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.PageEdit
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SaveEditedPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<SaveEditedPdfUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val pageEdits: List<PageEdit>,
        val outputUri: Uri,
        val password: String? = null
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.saveEditedPdf(params.inputUri, params.outputUri, params.pageEdits, params.password)
}
