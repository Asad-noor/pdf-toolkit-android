package com.offlinepdf.toolkit.core.data.source

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.PdfDocument
import com.offlinepdf.toolkit.core.domain.model.PdfPage
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig

interface PdfDataSource {
    suspend fun getDocumentInfo(bytes: ByteArray, uri: Uri, fileName: String, fileSize: Long, password: String?): PdfDocument
    suspend fun getPageList(bytes: ByteArray, password: String?): List<PdfPage>
    suspend fun merge(inputs: List<ByteArray>, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun split(input: ByteArray, mode: SplitMode, password: String?, onProgress: (Int, Int) -> Unit): List<ByteArray>
    suspend fun compress(input: ByteArray, level: CompressionLevel, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun rotatePages(input: ByteArray, pageNumbers: List<Int>?, rotation: RotationDegree, password: String?, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun extractPages(input: ByteArray, pageNumbers: List<Int>, password: String?, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun addWatermark(input: ByteArray, config: WatermarkConfig, imageBytes: ByteArray?, password: String?, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun encrypt(input: ByteArray, config: PasswordConfig, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun unlock(input: ByteArray, currentPassword: String, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun imagesToPdf(imageBytesList: List<ByteArray>, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun extractText(input: ByteArray, password: String?, onProgress: (Int, Int) -> Unit): String
    suspend fun reorderPages(input: ByteArray, newOrder: List<Int>, password: String?, onProgress: (Int, Int) -> Unit): ByteArray
    suspend fun deletePages(input: ByteArray, pageNumbers: List<Int>, password: String?, onProgress: (Int, Int) -> Unit): ByteArray
}
