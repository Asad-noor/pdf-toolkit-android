package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.PdfOperationResult
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RenderPageUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) {
    data class Params(val uri: Uri, val pageNumber: Int, val dpi: Int = 150, val password: String? = null)

    operator fun invoke(params: Params): Flow<PdfOperationResult> =
        pdfRepository.renderPage(params.uri, params.pageNumber, params.dpi, params.password)
}
