package com.offlinepdf.toolkit.domain.usecase

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.pdf.SplitPdfUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SplitPdfUseCaseTest {

    private lateinit var repository: PdfRepository
    private lateinit var useCase: SplitPdfUseCase

    private val inputUri: Uri = mockk(relaxed = true)
    private val outputDirUri: Uri = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = mockk()
        useCase = SplitPdfUseCase(repository)
    }

    @Test
    fun `EveryN mode delegates to repository correctly`() = runTest {
        val mode = SplitMode.EveryN(3)
        val expected = listOf(ProcessingProgress(1, 1, ProcessingProgress.Phase.DONE))
        every { repository.splitPdf(inputUri, mode, outputDirUri, null) } returns flowOf(*expected.toTypedArray())

        val result = useCase(SplitPdfUseCase.Params(inputUri, mode, outputDirUri)).toList()

        assertEquals(expected, result)
        verify { repository.splitPdf(inputUri, mode, outputDirUri, null) }
    }

    @Test
    fun `ByRange mode passes ranges correctly`() = runTest {
        val mode = SplitMode.ByRange(listOf(1..3, 4..6))
        every { repository.splitPdf(inputUri, mode, outputDirUri, null) } returns flowOf(
            ProcessingProgress(2, 2, ProcessingProgress.Phase.DONE)
        )

        useCase(SplitPdfUseCase.Params(inputUri, mode, outputDirUri)).toList()

        verify { repository.splitPdf(inputUri, mode, outputDirUri, null) }
    }

    @Test
    fun `AtPages mode passes page numbers correctly`() = runTest {
        val mode = SplitMode.AtPages(listOf(4, 8))
        every { repository.splitPdf(inputUri, mode, outputDirUri, "pass") } returns flowOf(
            ProcessingProgress(1, 1, ProcessingProgress.Phase.DONE)
        )

        useCase(SplitPdfUseCase.Params(inputUri, mode, outputDirUri, "pass")).toList()

        verify { repository.splitPdf(inputUri, mode, outputDirUri, "pass") }
    }

    @Test
    fun `progress fraction is calculated correctly`() {
        val progress = ProcessingProgress(3, 6, ProcessingProgress.Phase.PROCESSING)
        assertEquals(0.5f, progress.fraction)
    }

    @Test
    fun `progress fraction is zero when total is zero`() {
        val progress = ProcessingProgress(0, 0, ProcessingProgress.Phase.READING)
        assertEquals(0f, progress.fraction)
    }
}
