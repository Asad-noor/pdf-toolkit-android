package com.example.pdf_utility_app.ui.merge

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.pdf_utility_app.ui.components.PdfTopAppBar
import com.example.pdf_utility_app.ui.components.ProgressContent
import com.example.pdf_utility_app.ui.components.SuccessContent

@Composable
fun MergePdfsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MergeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val multiPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}
            }
            selectedUris = uris
            selectedNames = uris.map { it.lastPathSegment?.substringAfterLast('/') ?: "unknown.pdf" }
            viewModel.onFilesSelected(uris, selectedNames)
        }
    }

    val saveDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { outputUri ->
        if (outputUri != null && selectedUris.isNotEmpty()) {
            viewModel.merge(selectedUris, outputUri)
        }
    }

    when (val state = uiState) {
        is MergeUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Merging PDFs",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is MergeUiState.Success -> SuccessContent(
            operationName = "PDFs merged successfully",
            outputInfo = "Output saved to: ${state.outputUri.lastPathSegment}",
            onNewOperation = { viewModel.reset(); onBack() },
            modifier = modifier
        )
        is MergeUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        else -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Merge PDFs", onBack = onBack) },
                bottomBar = {
                    BottomActionBar(
                        primaryText = "Merge",
                        enabled = selectedUris.size >= 2,
                        onPrimary = { saveDocLauncher.launch("merged.pdf") },
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
                    OutlinedButton(
                        onClick = { multiPdfLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add PDF Files")
                    }

                    if (selectedNames.isEmpty()) {
                        Text(
                            "Select 2 or more PDF files to merge.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Selected Files (${selectedNames.size}):",
                            style = MaterialTheme.typography.titleSmall
                        )
                        selectedNames.forEachIndexed { index, name ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    "${index + 1}. $name",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Files will be merged in the order shown above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
