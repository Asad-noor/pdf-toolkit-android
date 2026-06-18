package com.offlinepdf.toolkit.di

import android.content.Context
import com.offlinepdf.toolkit.core.data.processor.AndroidPdfRenderer
import com.offlinepdf.toolkit.core.data.processor.ITextPdfProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PdfModule {

    @Provides
    @Singleton
    fun provideITextProcessor(): ITextPdfProcessor = ITextPdfProcessor()

    @Provides
    @Singleton
    fun provideAndroidRenderer(@ApplicationContext ctx: Context): AndroidPdfRenderer =
        AndroidPdfRenderer(ctx)
}
