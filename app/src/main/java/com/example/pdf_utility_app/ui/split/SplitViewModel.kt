package com.example.pdf_utility_app.ui.split

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.SplitMode
import com.offlinepdf.toolkit.core.domain.usecase.pdf.SplitPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplitUiState {
    object Idle : SplitUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : SplitUiState()
    data class Processing(val progress: ProcessingProgress) : SplitUiState()
    data class Success(val message: String) : SplitUiState()
    data class Error(val message: String) : SplitUiState()
}

@HiltViewModel
class SplitViewModel @Inject constructor(
    private val splitUseCase: SplitPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplitUiState>(SplitUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = SplitUiState.Ready(uri, name)
    }

    fun split(outputDirUri: Uri, mode: SplitMode) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                splitUseCase(SplitPdfUseCase.Params(uri, mode, outputDirUri))
                    .collect { progress ->
                        _uiState.value = SplitUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = SplitUiState.Success("PDF split successfully into output directory.")
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = SplitUiState.Error(e.message ?: "Split failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = SplitUiState.Ready(it, "") } ?: run { _uiState.value = SplitUiState.Idle }
    }

    fun reset() { _uiState.value = SplitUiState.Idle; inputUri = null }
}
