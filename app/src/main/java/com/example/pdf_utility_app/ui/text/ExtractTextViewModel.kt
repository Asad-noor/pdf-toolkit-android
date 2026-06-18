package com.example.pdf_utility_app.ui.text

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ExtractTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExtractTextUiState {
    object Idle : ExtractTextUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : ExtractTextUiState()
    data class Processing(val progress: ProcessingProgress) : ExtractTextUiState()
    data class Success(val text: String) : ExtractTextUiState()
    data class Error(val message: String) : ExtractTextUiState()
}

@HiltViewModel
class ExtractTextViewModel @Inject constructor(
    private val extractTextUseCase: ExtractTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExtractTextUiState>(ExtractTextUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = ExtractTextUiState.Ready(uri, name)
    }

    fun extractText() {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                extractTextUseCase(ExtractTextUseCase.Params(uri))
                    .collect { progress ->
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = ExtractTextUiState.Success(progress.message ?: "")
                        } else {
                            _uiState.value = ExtractTextUiState.Processing(progress)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = ExtractTextUiState.Error(e.message ?: "Text extraction failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = ExtractTextUiState.Ready(it, "") }
            ?: run { _uiState.value = ExtractTextUiState.Idle }
    }

    fun reset() {
        _uiState.value = ExtractTextUiState.Idle
        inputUri = null
    }
}
