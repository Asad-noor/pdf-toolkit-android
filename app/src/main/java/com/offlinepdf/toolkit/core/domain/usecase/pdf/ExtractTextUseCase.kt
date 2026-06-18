package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExtractTextUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<ExtractTextUseCase.Params>() {

    data class Params(val inputUri: Uri, val password: String? = null)

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.extractText(params.inputUri, params.password)
}
