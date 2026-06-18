package com.example.pdf_utility_app.ui.split

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.offlinepdf.toolkit.core.domain.model.SplitMode

@Composable
fun SplitPdfScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplitViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var inputName by remember { mutableStateOf<String?>(null) }
    var splitByEveryN by remember { mutableStateOf(true) }
    var everyNValue by remember { mutableStateOf("2") }
    var atPagesValue by remember { mutableStateOf("") }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) {}
            inputUri = uri
            inputName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            viewModel.onFileSelected(uri, inputName!!)
        }
    }

    val dirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { dirUri ->
        if (dirUri != null && inputUri != null) {
            val mode = if (splitByEveryN) {
                SplitMode.EveryN(everyNValue.toIntOrNull()?.coerceAtLeast(1) ?: 1)
            } else {
                SplitMode.AtPages(parsePageNumbers(atPagesValue))
            }
            viewModel.split(dirUri, mode)
        }
    }

    when (val state = uiState) {
        is SplitUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Splitting PDF",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is SplitUiState.Success -> SuccessContent(
            operationName = "PDF split successfully",
            outputInfo = state.message,
            onNewOperation = { viewModel.reset(); onBack() },
            modifier = modifier
        )
        is SplitUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        else -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Split PDF", onBack = onBack) },
                bottomBar = {
                    val canSplit = inputUri != null && (
                        (splitByEveryN && (everyNValue.toIntOrNull() ?: 0) > 0) ||
                        (!splitByEveryN && atPagesValue.isNotBlank())
                    )
                    BottomActionBar(
                        primaryText = "Choose Output Folder",
                        enabled = canSplit,
                        onPrimary = { dirLauncher.launch(null) },
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

                    Text("Split Mode", style = MaterialTheme.typography.titleSmall)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = splitByEveryN, onClick = { splitByEveryN = true })
                        Text("Every N pages", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (splitByEveryN) {
                        OutlinedTextField(
                            value = everyNValue,
                            onValueChange = { everyNValue = it },
                            label = { Text("Pages per chunk") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = !splitByEveryN, onClick = { splitByEveryN = false })
                        Text("At specific pages", modifier = Modifier.padding(start = 8.dp))
                    }
                    if (!splitByEveryN) {
                        OutlinedTextField(
                            value = atPagesValue,
                            onValueChange = { atPagesValue = it },
                            label = { Text("Split before pages (e.g. 4,8,12)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Enter page numbers where splits should occur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
