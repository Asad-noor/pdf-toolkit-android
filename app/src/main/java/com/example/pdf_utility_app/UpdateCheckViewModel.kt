package com.example.pdf_utility_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.update.UpdateCheckService
import com.offlinepdf.toolkit.update.UpdateInfo
import com.worldvisionsoft.pdftoolkit.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val updateCheckService: UpdateCheckService
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val info = updateCheckService.fetchUpdateInfo() ?: return@launch
            if (updateCheckService.isUpdateAvailable(BuildConfig.VERSION_NAME, info.latestVersion)) {
                _updateInfo.value = info
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }
}
