package com.example.pdf_utility_app.ui.images

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ImagesToPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ImagesToPdfUiState {
    object Idle : ImagesToPdfUiState()
    data class ImagesReady(val uris: List<Uri>) : ImagesToPdfUiState()
    data class Processing(val progress: ProcessingProgress) : ImagesToPdfUiState()
    data class Success(val outputUri: Uri) : ImagesToPdfUiState()
    data class Error(val message: String) : ImagesToPdfUiState()
}

@HiltViewModel
class ImagesToPdfViewModel @Inject constructor(
    private val imagesToPdfUseCase: ImagesToPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImagesToPdfUiState>(ImagesToPdfUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var operationJob: Job? = null

    fun onImagesSelected(uris: List<Uri>) {
        _uiState.value = ImagesToPdfUiState.ImagesReady(uris)
    }

    fun convert(imageUris: List<Uri>, outputUri: Uri) {
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                imagesToPdfUseCase(ImagesToPdfUseCase.Params(imageUris, outputUri))
                    .collect { progress ->
                        _uiState.value = ImagesToPdfUiState.Processing(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = ImagesToPdfUiState.Success(outputUri)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = ImagesToPdfUiState.Error(e.message ?: "Conversion failed")
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        _uiState.value = ImagesToPdfUiState.Idle
    }

    fun reset() {
        _uiState.value = ImagesToPdfUiState.Idle
    }
}
