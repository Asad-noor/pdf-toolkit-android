package com.offlinepdf.toolkit.core.domain.usecase.base

import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

abstract class FlowUseCase<in P> {
    operator fun invoke(params: P): Flow<ProcessingProgress> = execute(params)
        .catch { e ->
            if (e is CancellationException) throw e
            emit(ProcessingProgress(0, 0, ProcessingProgress.Phase.DONE, "Error: ${e.message}"))
        }

    protected abstract fun execute(params: P): Flow<ProcessingProgress>
}
