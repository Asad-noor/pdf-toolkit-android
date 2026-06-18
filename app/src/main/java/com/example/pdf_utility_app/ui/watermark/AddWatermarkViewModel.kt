package com.example.pdf_utility_app.ui.watermark

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.WatermarkConfig
import com.offlinepdf.toolkit.core.domain.model.WatermarkPosition
import com.offlinepdf.toolkit.core.domain.usecase.pdf.AddWatermarkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddWatermarkUiState {
    object Idle : AddWatermarkUiState()
    data class Ready(val inputUri: Uri, val inputName: String) : AddWatermarkUiState()
    data class Processing(val progress: ProcessingProgress) : AddWatermarkUiState()
    data class Success(val outputUri: Uri) : AddWatermarkUiState()
    data class Error(val message: String) : AddWatermarkUiState()
}

@HiltViewModel
class AddWatermarkViewModel @Inject constructor(
    private val addWatermarkUseCase: AddWatermarkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddWatermarkUiState>(AddWatermarkUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var inputUri: Uri? = null
    private var operationJob: Job? = null

    fun onFileSelected(uri: Uri, name: String) {
        inputUri = uri
        _uiState.value = AddWatermarkUiState.Ready(uri, name)
    }

    fun addWatermark(
        outputUri: Uri,
        text: String,
        opacity: Float,
        position: WatermarkPosition
    ) {
        val uri = inputUri ?: return
        operationJob?.cancel()
        val config = WatermarkConfig.TextWatermark(
            text = text,
            opacity = opacity,
            position = position
        )
        operationJob = viewModelScope.launch {
            try {
                addWatermarkUseCase(AddWatermarkUseCase.Params(uri, outputUri, config))
                    .collect { progress ->
                        _uiState.value = AddWatermarkUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = AddWatermarkUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = AddWatermarkUiState.Error(e.message ?: "Watermark failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        inputUri?.let { _uiState.value = AddWatermarkUiState.Ready(it, "") }
            ?: run { _uiState.value = AddWatermarkUiState.Idle }
    }

    fun reset() {
        _uiState.value = AddWatermarkUiState.Idle
        inputUri = null
    }
}
