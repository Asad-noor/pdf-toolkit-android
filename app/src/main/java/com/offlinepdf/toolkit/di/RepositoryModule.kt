package com.offlinepdf.toolkit.di

import com.offlinepdf.toolkit.core.data.repository.FileRepositoryImpl
import com.offlinepdf.toolkit.core.data.repository.PdfRepositoryImpl
import com.offlinepdf.toolkit.core.domain.repository.FileRepository
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPdfRepository(impl: PdfRepositoryImpl): PdfRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository
}
