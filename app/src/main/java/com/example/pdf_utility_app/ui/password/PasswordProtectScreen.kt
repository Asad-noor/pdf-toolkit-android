package com.example.pdf_utility_app.ui.password

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdf_utility_app.ui.components.BottomActionBar
import com.example.pdf_utility_app.ui.components.ErrorContent
import com.example.pdf_utility_app.ui.components.InputFileSection
import com.example.pdf_utility_app.ui.components.PdfTopAppBar
import com.example.pdf_utility_app.ui.components.ProgressContent
import com.example.pdf_utility_app.ui.components.SuccessContent
import com.example.pdf_utility_app.ui.components.ValidatedPasswordField

@Composable
fun PasswordProtectScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordProtectViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var inputName by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            inputUri = uri
            inputName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            viewModel.onFileSelected(uri, inputName!!)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { outputUri ->
        if (outputUri != null) {
            viewModel.protect(outputUri, password)
        }
    }

    when (val state = uiState) {
        is PasswordProtectUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Protecting PDF",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is PasswordProtectUiState.Success -> SuccessContent(
            operationName = "PDF protected successfully",
            outputInfo = "Saved to: ${state.outputUri.lastPathSegment}",
            onNewOperation = { viewModel.reset(); onBack() },
            modifier = modifier
        )
        is PasswordProtectUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        else -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Password Protect", onBack = onBack) },
                bottomBar = {
                    val canProtect = inputUri != null &&
                        password.length >= 4 &&
                        password == confirmPassword
                    BottomActionBar(
                        primaryText = "Protect & Save",
                        enabled = canProtect,
                        onPrimary = { saveLauncher.launch("protected.pdf") },
                        onCancel = onBack
                    )
                },
                modifier = modifier
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InputFileSection(
                        label = "Input PDF",
                        fileName = inputName,
                        onPickFile = { fileLauncher.launch(arrayOf("application/pdf")) }
                    )
                    ValidatedPasswordField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        error = if (password.isNotEmpty() && password.length < 4)
                            "Password must be at least 4 characters" else null
                    )
                    ValidatedPasswordField(
                        label = "Confirm Password",
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        error = if (confirmPassword.isNotEmpty() && password != confirmPassword)
                            "Passwords do not match" else null
                    )
                }
            }
        }
    }
}
