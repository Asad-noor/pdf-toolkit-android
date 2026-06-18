package com.offlinepdf.toolkit.di

import androidx.work.WorkManager
import com.offlinepdf.toolkit.worker.PdfWorkerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun providePdfWorkerScheduler(
        workManager: WorkManager,
        json: Json
    ): PdfWorkerScheduler = PdfWorkerScheduler(workManager, json)
}
