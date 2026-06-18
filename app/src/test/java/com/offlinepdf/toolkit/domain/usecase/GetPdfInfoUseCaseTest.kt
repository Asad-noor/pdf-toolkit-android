package com.offlinepdf.toolkit.domain.usecase

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.PdfDocument
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.pdf.GetPdfInfoUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetPdfInfoUseCaseTest {

    private lateinit var repository: PdfRepository
    private lateinit var useCase: GetPdfInfoUseCase

    private val testUri: Uri = mockk(relaxed = true)
    private val testDoc = PdfDocument(
        uri = "content://test/doc.pdf",
        fileName = "doc.pdf",
        pageCount = 5,
        fileSizeBytes = 1024L,
        isPasswordProtected = false,
        author = "Test Author",
        title = "Test PDF"
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetPdfInfoUseCase(repository)
    }

    @Test
    fun `invoke returns success with document info`() = runTest {
        coEvery { repository.getDocumentInfo(testUri, null) } returns Result.success(testDoc)

        val result = useCase(GetPdfInfoUseCase.Params(testUri))

        assertTrue(result.isSuccess)
        assertEquals(testDoc, result.getOrNull())
        coVerify(exactly = 1) { repository.getDocumentInfo(testUri, null) }
    }

    @Test
    fun `invoke passes password to repository`() = runTest {
        val password = "secret"
        coEvery { repository.getDocumentInfo(testUri, password) } returns Result.success(testDoc)

        val result = useCase(GetPdfInfoUseCase.Params(testUri, password))

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.getDocumentInfo(testUri, password) }
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        val exception = RuntimeException("File not found")
        coEvery { repository.getDocumentInfo(testUri, null) } returns Result.failure(exception)

        val result = useCase(GetPdfInfoUseCase.Params(testUri))

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke wraps unexpected exceptions as failure`() = runTest {
        coEvery { repository.getDocumentInfo(testUri, null) } throws RuntimeException("Unexpected")

        val result = useCase(GetPdfInfoUseCase.Params(testUri))

        assertTrue(result.isFailure)
    }
}
