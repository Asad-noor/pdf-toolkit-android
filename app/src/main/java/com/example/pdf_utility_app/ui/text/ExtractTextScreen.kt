package com.example.pdf_utility_app.ui.text

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdf_utility_app.ui.components.BottomActionBar
import com.example.pdf_utility_app.ui.components.ErrorContent
import com.example.pdf_utility_app.ui.components.InputFileSection
import com.example.pdf_utility_app.ui.components.PdfTopAppBar
import com.example.pdf_utility_app.ui.components.ProgressContent
import kotlinx.coroutines.launch

@Composable
fun ExtractTextScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExtractTextViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var inputName by remember { mutableStateOf<String?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            inputName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            viewModel.onFileSelected(uri, inputName!!)
        }
    }

    when (val state = uiState) {
        is ExtractTextUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Extracting Text",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is ExtractTextUiState.Success -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Extract Text", onBack = onBack) },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = modifier
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Extracted Text", style = MaterialTheme.typography.titleSmall)
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(state.text))
                                scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Text("Copy", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = state.text.ifBlank { "No text found in this PDF." },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Extract Another File")
                    }
                }
            }
        }
        is ExtractTextUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        else -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Extract Text", onBack = onBack) },
                bottomBar = {
                    BottomActionBar(
                        primaryText = "Extract Text",
                        enabled = (uiState is ExtractTextUiState.Ready),
                        onPrimary = { viewModel.extractText() },
                        onCancel = onBack
                    )
                },
                modifier = modifier
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InputFileSection(
                        label = "Input PDF",
                        fileName = inputName,
                        onPickFile = { fileLauncher.launch(arrayOf("application/pdf")) }
                    )
                    Text(
                        "Text will be extracted from all pages of the PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
