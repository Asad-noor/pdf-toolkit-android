package com.example.pdf_utility_app.ui.extract

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ExtractPagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExtractPagesUiState {
    object Idle : ExtractPagesUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : ExtractPagesUiState()
    data class Processing(val progress: ProcessingProgress) : ExtractPagesUiState()
    data class Success(val outputUri: Uri) : ExtractPagesUiState()
    data class Error(val message: String) : ExtractPagesUiState()
}

@HiltViewModel
class ExtractPagesViewModel @Inject constructor(
    private val extractPagesUseCase: ExtractPagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExtractPagesUiState>(ExtractPagesUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = ExtractPagesUiState.Ready(uri, name)
    }

    fun extract(outputUri: Uri, pageNumbers: List<Int>) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                extractPagesUseCase(ExtractPagesUseCase.Params(uri, outputUri, pageNumbers))
                    .collect { progress ->
                        _uiState.value = ExtractPagesUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = ExtractPagesUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = ExtractPagesUiState.Error(e.message ?: "Extraction failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = ExtractPagesUiState.Ready(it, "") } ?: run { _uiState.value = ExtractPagesUiState.Idle }
    }

    fun reset() { _uiState.value = ExtractPagesUiState.Idle; inputUri = null }
}
