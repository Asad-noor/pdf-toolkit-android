package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.TextRegion
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import javax.inject.Inject

class ExtractPdfTextWithFontUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) {
    data class Params(val pdfUri: Uri, val password: String? = null)

    suspend operator fun invoke(params: Params): Result<List<TextRegion>> =
        pdfRepository.extractTextWithFont(params.pdfUri, params.password)
}
