package com.offlinepdf.toolkit.core.domain.repository

import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface FileRepository {
    suspend fun openInputStream(uri: Uri): Result<InputStream>
    suspend fun openOutputStream(uri: Uri): Result<OutputStream>
    suspend fun getFileSize(uri: Uri): Result<Long>
    suspend fun getFileName(uri: Uri): Result<String>
    suspend fun copyToTemp(uri: Uri): Result<File>
    suspend fun createOutputUri(dirUri: Uri, fileName: String, mimeType: String): Result<Uri>
    suspend fun deleteTempFiles()
}
