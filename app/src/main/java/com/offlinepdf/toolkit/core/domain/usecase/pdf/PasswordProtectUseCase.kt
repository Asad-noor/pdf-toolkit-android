package com.offlinepdf.toolkit.core.domain.usecase.pdf

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PasswordProtectUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<PasswordProtectUseCase.Params>() {

    data class Params(
        val inputUri: Uri,
        val outputUri: Uri,
        val config: PasswordConfig
    )

    override fun execute(params: Params): Flow<ProcessingProgress> =
        pdfRepository.passwordProtect(params.inputUri, params.outputUri, params.config)
}
