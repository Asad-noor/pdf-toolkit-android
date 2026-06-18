package com.offlinepdf.toolkit.core.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.offlinepdf.toolkit.core.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {

    private val tempDir: File
        get() = File(context.cacheDir, "pdf_temp").also { it.mkdirs() }

    override suspend fun openInputStream(uri: Uri): Result<InputStream> = runCatching {
        context.contentResolver.openInputStream(uri)
            ?: error("Cannot open input stream for $uri")
    }

    override suspend fun openOutputStream(uri: Uri): Result<OutputStream> = runCatching {
        context.contentResolver.openOutputStream(uri)
            ?: error("Cannot open output stream for $uri")
    }

    override suspend fun getFileSize(uri: Uri): Result<Long> = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex != -1) {
                cursor.getLong(sizeIndex)
            } else -1L
        } ?: -1L
    }

    override suspend fun getFileName(uri: Uri): Result<String> = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                cursor.getString(nameIndex)
            } else uri.lastPathSegment ?: "unknown.pdf"
        } ?: (uri.lastPathSegment ?: "unknown.pdf")
    }

    override suspend fun copyToTemp(uri: Uri): Result<File> = runCatching {
        val fileName = getFileName(uri).getOrElse { "temp_${System.currentTimeMillis()}.pdf" }
        val tempFile = File(tempDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open input stream for $uri")
        tempFile
    }

    override suspend fun createOutputUri(dirUri: Uri, fileName: String, mimeType: String): Result<Uri> = runCatching {
        DocumentsContract.createDocument(context.contentResolver, dirUri, mimeType, fileName)
            ?: error("Failed to create document $fileName in $dirUri")
    }

    override suspend fun deleteTempFiles() {
        tempDir.listFiles()?.forEach { it.delete() }
    }
}
