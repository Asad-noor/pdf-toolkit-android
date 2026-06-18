package com.offlinepdf.toolkit.core.domain.usecase.base

import kotlinx.coroutines.CancellationException

abstract class UseCase<in P, R> {
    suspend operator fun invoke(params: P): Result<R> {
        return try {
            execute(params)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    protected abstract suspend fun execute(params: P): Result<R>
}
