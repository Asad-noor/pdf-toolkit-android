package com.offlinepdf.toolkit.core.data.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPdfRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mutex = Mutex()
    private var currentUri: Uri? = null
    private var currentPfd: ParcelFileDescriptor? = null
    private var currentRenderer: PdfRenderer? = null

    suspend fun renderPage(uri: Uri, pageNumber: Int, dpi: Int): Result<Bitmap> = mutex.withLock {
        return runCatching {
            ensureRendererOpen(uri)
            val renderer = currentRenderer ?: error("Renderer unavailable")
            if (pageNumber < 0 || pageNumber >= renderer.pageCount) {
                error("Page $pageNumber out of bounds (count=${renderer.pageCount})")
            }
            renderer.openPage(pageNumber).use { page ->
                val scale = dpi / 72f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    suspend fun getPageCount(uri: Uri): Result<Int> = mutex.withLock {
        runCatching {
            ensureRendererOpen(uri)
            currentRenderer?.pageCount ?: error("Renderer unavailable")
        }
    }

    suspend fun getPageDimensions(uri: Uri, pageNumber: Int): Result<Pair<Float, Float>> = mutex.withLock {
        runCatching {
            ensureRendererOpen(uri)
            val renderer = currentRenderer ?: error("Renderer unavailable")
            renderer.openPage(pageNumber).use { page ->
                Pair(page.width.toFloat(), page.height.toFloat())
            }
        }
    }

    private fun ensureRendererOpen(uri: Uri) {
        if (currentUri == uri && currentRenderer != null) return
        closeRenderer()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Cannot open file descriptor for $uri")
        currentPfd = pfd
        currentRenderer = PdfRenderer(pfd)
        currentUri = uri
    }

    private fun closeRenderer() {
        currentRenderer?.close()
        currentRenderer = null
        currentPfd?.close()
        currentPfd = null
        currentUri = null
    }

    fun close() {
        closeRenderer()
    }
}
