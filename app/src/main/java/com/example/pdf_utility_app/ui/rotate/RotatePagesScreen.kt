package com.example.pdf_utility_app.ui.rotate

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
import androidx.compose.material3.Checkbox
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
import com.offlinepdf.toolkit.core.domain.model.RotationDegree

@Composable
fun RotatePagesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RotatePagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var inputName by remember { mutableStateOf<String?>(null) }
    var rotateAll by remember { mutableStateOf(true) }
    var pagesInput by remember { mutableStateOf("") }
    var selectedRotation by remember { mutableStateOf(RotationDegree.CLOCKWISE_90) }

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
            val pages = if (rotateAll) null else parsePageNumbers(pagesInput)
            viewModel.rotate(outputUri, pages, selectedRotation)
        }
    }

    when (val state = uiState) {
        is RotatePagesUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Rotating Pages",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is RotatePagesUiState.Success -> SuccessContent(
            operationName = "Pages rotated successfully",
            outputInfo = "Saved to: ${state.outputUri.lastPathSegment}",
            onNewOperation = { viewModel.reset(); onBack() },
            modifier = modifier
        )
        is RotatePagesUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        else -> {
            Scaffold(
                topBar = { PdfTopAppBar(title = "Rotate Pages", onBack = onBack) },
                bottomBar = {
                    val canRotate = inputUri != null && (rotateAll || pagesInput.isNotBlank())
                    BottomActionBar(
                        primaryText = "Rotate & Save",
                        enabled = canRotate,
                        onPrimary = { saveLauncher.launch("rotated.pdf") },
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = rotateAll,
                            onCheckedChange = { rotateAll = it }
                        )
                        Text("Rotate all pages", modifier = Modifier.padding(start = 8.dp))
                    }

                    if (!rotateAll) {
                        OutlinedTextField(
                            value = pagesInput,
                            onValueChange = { pagesInput = it },
                            label = { Text("Pages to rotate (e.g. 1,3,5-7)") },
                            placeholder = { Text("1,3,5-7") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Text("Rotation", style = MaterialTheme.typography.titleSmall)

                    listOf(
                        RotationDegree.CLOCKWISE_90 to "90° Clockwise",
                        RotationDegree.CLOCKWISE_180 to "180°",
                        RotationDegree.CLOCKWISE_270 to "270° Clockwise (90° Counter-clockwise)"
                    ).forEach { (degree, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedRotation == degree,
                                onClick = { selectedRotation = degree }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
