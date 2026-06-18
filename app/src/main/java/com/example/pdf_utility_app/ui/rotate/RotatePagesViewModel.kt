package com.example.pdf_utility_app.ui.rotate

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.RotationDegree
import com.offlinepdf.toolkit.core.domain.usecase.pdf.RotatePageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RotatePagesUiState {
    object Idle : RotatePagesUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : RotatePagesUiState()
    data class Processing(val progress: ProcessingProgress) : RotatePagesUiState()
    data class Success(val outputUri: Uri) : RotatePagesUiState()
    data class Error(val message: String) : RotatePagesUiState()
}

@HiltViewModel
class RotatePagesViewModel @Inject constructor(
    private val rotatePageUseCase: RotatePageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RotatePagesUiState>(RotatePagesUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = RotatePagesUiState.Ready(uri, name)
    }

    fun rotate(outputUri: Uri, pageNumbers: List<Int>?, rotation: RotationDegree) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                rotatePageUseCase(RotatePageUseCase.Params(uri, outputUri, pageNumbers, rotation))
                    .collect { progress ->
                        _uiState.value = RotatePagesUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = RotatePagesUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = RotatePagesUiState.Error(e.message ?: "Rotation failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = RotatePagesUiState.Ready(it, "") }
            ?: run { _uiState.value = RotatePagesUiState.Idle }
    }

    fun reset() {
        _uiState.value = RotatePagesUiState.Idle
        inputUri = null
    }
}
