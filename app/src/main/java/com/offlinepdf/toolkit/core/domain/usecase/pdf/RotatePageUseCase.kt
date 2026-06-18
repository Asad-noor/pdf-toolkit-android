package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RotatePageUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<RotatePageUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val outputUri: Uri,
        val pageNumbers: List<Int>?,
        val rotation: RotationDegree,
        val password: String? = null
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.rotatePages(params.inputUri, params.outputUri, params.pageNumbers, params.rotation, params.password)
}
