package com.example.pdf_utility_app.ui.password

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.PasswordConfig
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.PasswordProtectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PasswordProtectUiState {
    object Idle : PasswordProtectUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : PasswordProtectUiState()
    data class Processing(val progress: ProcessingProgress) : PasswordProtectUiState()
    data class Success(val outputUri: Uri) : PasswordProtectUiState()
    data class Error(val message: String) : PasswordProtectUiState()
}

@HiltViewModel
class PasswordProtectViewModel @Inject constructor(
    private val passwordProtectUseCase: PasswordProtectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasswordProtectUiState>(PasswordProtectUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = PasswordProtectUiState.Ready(uri, name)
    }

    fun protect(outputUri: Uri, password: String) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                passwordProtectUseCase(
                    PasswordProtectUseCase.Params(uri, outputUri, PasswordConfig(userPassword = password))
                ).collect { progress ->
                    _uiState.value = PasswordProtectUiState.Processing(progress)
                    if (progress.phase == ProcessingProgress.Phase.DONE) {
                        _uiState.value = PasswordProtectUiState.Success(outputUri)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = PasswordProtectUiState.Error(e.message ?: "Protection failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = PasswordProtectUiState.Ready(it, "") }
            ?: run { _uiState.value = PasswordProtectUiState.Idle }
    }

    fun reset() {
        _uiState.value = PasswordProtectUiState.Idle
        inputUri = null
    }
}
