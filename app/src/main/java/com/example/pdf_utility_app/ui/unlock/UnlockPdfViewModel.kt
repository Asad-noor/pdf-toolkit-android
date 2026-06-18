package com.example.pdf_utility_app.ui.unlock

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.UnlockPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UnlockPdfUiState {
    object Idle : UnlockPdfUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : UnlockPdfUiState()
    data class Processing(val progress: ProcessingProgress) : UnlockPdfUiState()
    data class Success(val outputUri: Uri) : UnlockPdfUiState()
    data class Error(val message: String) : UnlockPdfUiState()
}

@HiltViewModel
class UnlockPdfViewModel @Inject constructor(
    private val unlockPdfUseCase: UnlockPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UnlockPdfUiState>(UnlockPdfUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = UnlockPdfUiState.Ready(uri, name)
    }

    fun unlock(outputUri: Uri, currentPassword: String) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                unlockPdfUseCase(UnlockPdfUseCase.Params(uri, outputUri, currentPassword))
                    .collect { progress ->
                        _uiState.value = UnlockPdfUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = UnlockPdfUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = UnlockPdfUiState.Error(e.message ?: "Unlock failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = UnlockPdfUiState.Ready(it, "") }
            ?: run { _uiState.value = UnlockPdfUiState.Idle }
    }

    fun reset() {
        _uiState.value = UnlockPdfUiState.Idle
        inputUri = null
    }
}
