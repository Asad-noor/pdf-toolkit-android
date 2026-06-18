package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReorderPagesUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<ReorderPagesUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val outputUri: Uri,
        val newOrder: List<Int>,
        val password: String? = null
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.reorderPages(params.inputUri, params.outputUri, params.newOrder, params.password)
}
