package com.example.pdf_utility_app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}

fun parsePageNumbers(input: String): List<Int> {
    return input.split(",").flatMap { part ->
        val trimmed = part.trim()
        if (trimmed.contains("-")) {
            val segments = trimmed.split("-")
            val start = segments.getOrNull(0)?.trim()?.toIntOrNull() ?: return@flatMap emptyList()
            val end = segments.getOrNull(1)?.trim()?.toIntOrNull() ?: return@flatMap emptyList()
            (start..end).toList()
        } else {
            listOfNotNull(trimmed.toIntOrNull())
        }
    }.distinct().sorted()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfTopAppBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { actions() }
    )
}

@Composable
fun BottomActionBar(
    primaryText: String,
    enabled: Boolean = true,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
        Button(
            onClick = onPrimary,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Text(primaryText)
        }
    }
}

@Composable
fun OperationCard(
    title: String,
    subtitle: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ProgressContent(
    progress: ProcessingProgress,
    operationName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(operationName, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${(progress.fraction * 100).toInt()}%",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Text(
                "Phase: ${progress.phase.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (progress.total > 0) {
                Text(
                    "${progress.current} / ${progress.total}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            progress.message?.takeIf { it.isNotBlank() }?.let { msg ->
                Text(msg, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun SuccessContent(
    operationName: String,
    outputInfo: String,
    onNewOperation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Text("Success!", style = MaterialTheme.typography.headlineMedium)
            Text(operationName, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    outputInfo,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNewOperation, modifier = Modifier.fillMaxWidth()) {
                Text("New Operation")
            }
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(72.dp)
            )
            Text("Operation Failed", style = MaterialTheme.typography.headlineMedium)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Back")
                }
                Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun InputFileSection(
    label: String,
    fileName: String?,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            fileName ?: "No file selected",
            style = MaterialTheme.typography.bodyMedium,
            color = if (fileName != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onPickFile) {
            Text("Choose File")
        }
    }
}

@Composable
fun ValidatedPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    var showPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password"
                )
            }
        },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}
