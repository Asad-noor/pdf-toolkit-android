package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UnlockPdfUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<UnlockPdfUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val outputUri: Uri,
        val currentPassword: String
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.unlockPdf(params.inputUri, params.outputUri, params.currentPassword)
}
