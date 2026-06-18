package com.example.pdf_utility_app.ui.merge

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.MergePdfsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MergeUiState {
    object Idle : MergeUiState()
    data class FilesReady(val uris: List<Uri>, val names: List<String>) : MergeUiState()
    data class Processing(val progress: ProcessingProgress) : MergeUiState()
    data class Success(val outputUri: Uri) : MergeUiState()
    data class Error(val message: String) : MergeUiState()
}

@HiltViewModel
class MergeViewModel @Inject constructor(
    private val mergeUseCase: MergePdfsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MergeUiState>(MergeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var operationJob: Job? = null

    fun onFilesSelected(uris: List<Uri>, names: List<String>) {
        _uiState.value = MergeUiState.FilesReady(uris, names)
    }

    fun merge(inputUris: List<Uri>, outputUri: Uri) {
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                mergeUseCase(MergePdfsUseCase.Params(inputUris, outputUri))
                    .collect { progress ->
                        _uiState.value = MergeUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = MergeUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = MergeUiState.Error(e.message ?: "Merge failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        _uiState.value = MergeUiState.Idle
    }

    fun reset() { _uiState.value = MergeUiState.Idle }
}
