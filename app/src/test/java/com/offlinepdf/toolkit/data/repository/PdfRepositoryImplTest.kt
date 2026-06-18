package com.offlinepdf.toolkit.data.repository

import android.content.Context
import android.net.Uri
import com.offlinepdf.toolkit.core.data.processor.AndroidPdfRenderer
import com.offlinepdf.toolkit.core.data.processor.ITextPdfProcessor
import com.offlinepdf.toolkit.core.data.repository.PdfRepositoryImpl
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.FileRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var processor: ITextPdfProcessor
    private lateinit var renderer: AndroidPdfRenderer
    private lateinit var fileRepository: FileRepository
    private lateinit var repository: PdfRepositoryImpl

    private val inputUri: Uri = mockk(relaxed = true)
    private val outputUri: Uri = mockk(relaxed = true)
    private val fakeBytes = "fake pdf content".toByteArray()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        processor = mockk(relaxed = true)
        renderer = mockk(relaxed = true)
        fileRepository = mockk()
        repository = PdfRepositoryImpl(context, processor, renderer, fileRepository)
    }

    @Test
    fun `getDocumentInfo returns success when all calls succeed`() = runTest {
        coEvery { fileRepository.openInputStream(inputUri) } returns
            Result.success(ByteArrayInputStream(fakeBytes))
        coEvery { fileRepository.getFileName(inputUri) } returns Result.success("test.pdf")
        coEvery { fileRepository.getFileSize(inputUri) } returns Result.success(1024L)
        every { processor.getPageCount(fakeBytes, null) } returns 5
        every { processor.getMetadata(fakeBytes, null) } returns mapOf("author" to "Test", "title" to null, "creationDate" to null)
        every { processor.isPasswordProtected(fakeBytes) } returns false

        val result = repository.getDocumentInfo(inputUri, null)

        assertTrue(result.isSuccess)
        val doc = result.getOrNull()!!
        assertEquals("test.pdf", doc.fileName)
        assertEquals(5, doc.pageCount)
        assertEquals(1024L, doc.fileSizeBytes)
        assertEquals(false, doc.isPasswordProtected)
    }

    @Test
    fun `getDocumentInfo returns failure when input stream fails`() = runTest {
        coEvery { fileRepository.openInputStream(inputUri) } returns
            Result.failure(Exception("Permission denied"))

        val result = repository.getDocumentInfo(inputUri, null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `compressPdf emits reading then done progress`() = runTest {
        coEvery { fileRepository.openInputStream(inputUri) } returns
            Result.success(ByteArrayInputStream(fakeBytes))
        val baos = ByteArrayOutputStream()
        coEvery { fileRepository.openOutputStream(outputUri) } returns Result.success(baos)
        every { processor.compress(fakeBytes, CompressionLevel.MEDIUM, "baos", any(), onProgress = any()) } returns Unit

        val progress = repository.compressPdf(inputUri, outputUri, CompressionLevel.MEDIUM).toList()

        assertTrue(progress.isNotEmpty())
        val first = progress.first()
        assertEquals(ProcessingProgress.Phase.READING, first.phase)
        val last = progress.last()
        assertEquals(ProcessingProgress.Phase.DONE, last.phase)
    }

    @Test
    fun `compressPdf emits error when input stream fails`() = runTest {
        coEvery { fileRepository.openInputStream(inputUri) } returns
            Result.failure(Exception("IO error"))

        val progress = repository.compressPdf(inputUri, outputUri, CompressionLevel.HIGH).toList()

        assertTrue(progress.isNotEmpty())
        assertEquals(ProcessingProgress.Phase.DONE, progress.last().phase)
    }
}
