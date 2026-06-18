package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MergePdfsUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<MergePdfsUseCase.Params>() {

    data class Params(val inputUris: List<Uri>, val outputUri: Uri)

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.mergePdfs(params.inputUris, params.outputUri)
}
