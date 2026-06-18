package com.example.pdf_utility_app.ui.extract

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.pdf_utility_app.ui.components.parsePageNumbers

@Composable
fun ExtractPagesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExtractPagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var inputName by remember { mutableStateOf<String?>(null) }
    var pagesInput by remember { mutableStateOf("") }

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
            viewModel.extract(outputUri, parsePageNumbers(pagesInput))
        }
    }

    when (val state = uiState) {
        is ExtractPagesUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Extracting Pages",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is ExtractPagesUiState.Success -> SuccessContent(
            operationName = "Pages extracted successfully",
            outputInfo = "Saved to: ${state.outputUri.lastPathSegment}",
            onNewOperation = { viewModel.reset(); onBack() },
            modifier = modifier
        )
        is ExtractPagesUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        else -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Extract Pages", onBack = onBack) },
                bottomBar = {
                    BottomActionBar(
                        primaryText = "Extract",
                        enabled = inputUri != null && pagesInput.isNotBlank(),
                        onPrimary = { saveLauncher.launch("extracted.pdf") },
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
                    OutlinedTextField(
                        value = pagesInput,
                        onValueChange = { pagesInput = it },
                        label = { Text("Pages to extract (e.g. 1,3,5-7)") },
                        placeholder = { Text("1,3,5-7") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        "Enter individual page numbers or ranges separated by commas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
