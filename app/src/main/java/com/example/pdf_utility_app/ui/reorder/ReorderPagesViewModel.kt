package com.example.pdf_utility_app.ui.reorder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ReorderPagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReorderPagesUiState {
    object Idle : ReorderPagesUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : ReorderPagesUiState()
    data class Processing(val progress: ProcessingProgress) : ReorderPagesUiState()
    data class Success(val outputUri: Uri) : ReorderPagesUiState()
    data class Error(val message: String) : ReorderPagesUiState()
}

@HiltViewModel
class ReorderPagesViewModel @Inject constructor(
    private val reorderPagesUseCase: ReorderPagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReorderPagesUiState>(ReorderPagesUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = ReorderPagesUiState.Ready(uri, name)
    }

    fun reorder(outputUri: Uri, newOrder: List<Int>) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                reorderPagesUseCase(ReorderPagesUseCase.Params(uri, outputUri, newOrder))
                    .collect { progress ->
                        _uiState.value = ReorderPagesUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = ReorderPagesUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = ReorderPagesUiState.Error(e.message ?: "Reorder failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = ReorderPagesUiState.Ready(it, "") }
            ?: run { _uiState.value = ReorderPagesUiState.Idle }
    }

    fun reset() {
        _uiState.value = ReorderPagesUiState.Idle
        inputUri = null
    }
}
