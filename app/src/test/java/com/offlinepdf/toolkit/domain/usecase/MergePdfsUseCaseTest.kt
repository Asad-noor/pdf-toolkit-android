package com.offlinepdf.toolkit.domain.usecase

import android.net.Uri
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.pdf.MergePdfsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MergePdfsUseCaseTest {

    private lateinit var repository: PdfRepository
    private lateinit var useCase: MergePdfsUseCase

    private val uri1: Uri = mockk(relaxed = true)
    private val uri2: Uri = mockk(relaxed = true)
    private val outputUri: Uri = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = mockk()
        useCase = MergePdfsUseCase(repository)
    }

    @Test
    fun `invoke emits progress events from repository`() = runTest {
        val progressEvents = listOf(
            ProcessingProgress(0, 2, ProcessingProgress.Phase.READING),
            ProcessingProgress(1, 2, ProcessingProgress.Phase.READING),
            ProcessingProgress(2, 2, ProcessingProgress.Phase.DONE)
        )
        every { repository.mergePdfs(listOf(uri1, uri2), outputUri) } returns flowOf(*progressEvents.toTypedArray())

        val params = MergePdfsUseCase.Params(listOf(uri1, uri2), outputUri)
        val result = useCase(params).toList()

        assertEquals(progressEvents, result)
        verify(exactly = 1) { repository.mergePdfs(listOf(uri1, uri2), outputUri) }
    }

    @Test
    fun `invoke emits error progress on exception`() = runTest {
        every { repository.mergePdfs(any(), any()) } returns flow {
            throw RuntimeException("Merge failed")
        }

        val params = MergePdfsUseCase.Params(listOf(uri1, uri2), outputUri)
        val result = useCase(params).toList()

        assertTrue(result.isNotEmpty())
        val last = result.last()
        assertEquals(ProcessingProgress.Phase.DONE, last.phase)
        assertTrue(last.message?.contains("Merge failed") == true)
    }

    @Test
    fun `invoke delegates correct uris to repository`() = runTest {
        val inputs = listOf(uri1, uri2)
        every { repository.mergePdfs(inputs, outputUri) } returns flowOf(
            ProcessingProgress(2, 2, ProcessingProgress.Phase.DONE)
        )

        val params = MergePdfsUseCase.Params(inputs, outputUri)
        useCase(params).toList()

        verify { repository.mergePdfs(inputs, outputUri) }
    }
}
