package com.example.pdf_utility_app.ui.images

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
fun ImagesToPdfScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImagesToPdfViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val imagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            }
            selectedUris = uris
            viewModel.onImagesSelected(uris)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { outputUri ->
        if (outputUri != null) {
            viewModel.convert(selectedUris, outputUri)
        }
    }

    when (val state = uiState) {
        is ImagesToPdfUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Converting Images to PDF",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is ImagesToPdfUiState.Success -> SuccessContent(
            operationName = "Images converted to PDF successfully",
            outputInfo = "Saved to: ${state.outputUri.lastPathSegment}",
            onNewOperation = { viewModel.reset(); onBack() },
            modifier = modifier
        )
        is ImagesToPdfUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        else -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Images to PDF", onBack = onBack) },
                bottomBar = {
                    BottomActionBar(
                        primaryText = "Convert to PDF",
                        enabled = selectedUris.isNotEmpty(),
                        onPrimary = { saveLauncher.launch("images.pdf") },
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
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        selectedUris.isEmpty() -> "No images selected"
                                        selectedUris.size == 1 -> "1 image selected"
                                        else -> "${selectedUris.size} images selected"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedUris.isEmpty())
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Button(onClick = { imagesLauncher.launch(arrayOf("image/*")) }) {
                                    Text(if (selectedUris.isEmpty()) "Select Images" else "Change")
                                }
                            }
                            if (selectedUris.isNotEmpty()) {
                                selectedUris.forEachIndexed { index, uri ->
                                    Text(
                                        text = "${index + 1}. ${uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Images will be placed one per page in the selected order.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
