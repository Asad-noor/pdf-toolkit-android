package com.example.pdf_utility_app.ui.delete

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.DeletePagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeletePagesUiState {
    object Idle : DeletePagesUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : DeletePagesUiState()
    data class Processing(val progress: ProcessingProgress) : DeletePagesUiState()
    data class Success(val outputUri: Uri) : DeletePagesUiState()
    data class Error(val message: String) : DeletePagesUiState()
}

@HiltViewModel
class DeletePagesViewModel @Inject constructor(
    private val deletePagesUseCase: DeletePagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeletePagesUiState>(DeletePagesUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = DeletePagesUiState.Ready(uri, name)
    }

    fun delete(outputUri: Uri, pageNumbers: List<Int>) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                deletePagesUseCase(DeletePagesUseCase.Params(uri, outputUri, pageNumbers))
                    .collect { progress ->
                        _uiState.value = DeletePagesUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = DeletePagesUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = DeletePagesUiState.Error(e.message ?: "Deletion failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = DeletePagesUiState.Ready(it, "") } ?: run { _uiState.value = DeletePagesUiState.Idle }
    }

    fun reset() { _uiState.value = DeletePagesUiState.Idle; inputUri = null }
}
