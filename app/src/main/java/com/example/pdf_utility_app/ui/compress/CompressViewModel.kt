package com.example.pdf_utility_app.ui.compress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.CompressionLevel
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.CompressPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CompressUiState {
    object Idle : CompressUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : CompressUiState()
    data class Processing(val progress: ProcessingProgress) : CompressUiState()
    data class Success(val outputUri: Uri) : CompressUiState()
    data class Error(val message: String) : CompressUiState()
}

@HiltViewModel
class CompressViewModel @Inject constructor(
    private val compressPdfUseCase: CompressPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompressUiState>(CompressUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = CompressUiState.Ready(uri, name)
    }

    fun compress(outputUri: Uri, level: CompressionLevel) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                compressPdfUseCase(CompressPdfUseCase.Params(uri, outputUri, level))
                    .collect { progress ->
                        _uiState.value = CompressUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = CompressUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = CompressUiState.Error(e.message ?: "Compression failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = CompressUiState.Ready(it, "") }
            ?: run { _uiState.value = CompressUiState.Idle }
    }

    fun reset() {
        _uiState.value = CompressUiState.Idle
        inputUri = null
    }
}
