package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.PdfDocument
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.UseCase
import javax.inject.Inject

class GetPdfInfoUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : UseCase<GetPdfInfoUseCase.Params, PdfDocument>() {

    data class Params(val uri: Uri, val password: String? = null)

    override suspend fun execute(params: Params): Result<PdfDocument> =
        pdfRepository.getDocumentInfo(params.uri, params.password)
}
