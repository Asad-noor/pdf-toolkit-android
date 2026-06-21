package com.example.pdf_utility_app.ui.edit

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdf_utility_app.ui.components.ErrorContent
import com.example.pdf_utility_app.ui.components.InputFileSection
import com.example.pdf_utility_app.ui.components.PdfTopAppBar
import com.example.pdf_utility_app.ui.components.ProgressContent
import com.example.pdf_utility_app.ui.components.SuccessContent

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun EditPdfScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditPdfViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.reset() }

    val inputLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
            viewModel.initializeEditor(uri)
        }
    }

    val outputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) viewModel.saveEdits(uri)
    }

    when (val state = uiState) {
        is EditPdfUiState.Idle -> IdleContent(
            onBack = onBack,
            onPickFile = { inputLauncher.launch(arrayOf("application/pdf")) },
            modifier = modifier
        )
        is EditPdfUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
        is EditPdfUiState.Ready -> EditorContent(
            state = state,
            onBack = onBack,
            onSave = {
                val base = state.pdfUri.lastPathSegment?.substringAfterLast('/')
                    ?.removeSuffix(".pdf") ?: "document"
                outputLauncher.launch("${base}_edited.pdf")
            },
            onSetMode = viewModel::setEditMode,
            onSetColor = viewModel::setColor,
            onSetBrush = viewModel::setBrushWidth,
            onStrokeAdded = viewModel::addStroke,
            onUndo = viewModel::undo,
            onClear = viewModel::clearPage,
            onCanvasTapped = viewModel::onCanvasTapped,
            onTextConfirmed = viewModel::confirmTextPlacement,
            onTextDismissed = viewModel::dismissTextDialog,
            onPreviousPage = viewModel::navigateToPreviousPage,
            onNextPage = viewModel::navigateToNextPage,
            modifier = modifier
        )
        is EditPdfUiState.Saving -> ProgressContent(
            progress = state.progress,
            operationName = "Saving Edited PDF",
            onCancel = {},
            modifier = modifier
        )
        is EditPdfUiState.Success -> SuccessContent(
            operationName = "PDF saved with your edits",
            outputInfo = state.outputUri.lastPathSegment ?: "edited.pdf",
            onNewOperation = viewModel::reset,
            modifier = modifier
        )
        is EditPdfUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = viewModel::reset,
            onBack = onBack,
            modifier = modifier
        )
    }
}

// ── Idle (file picker) ────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { PdfTopAppBar(title = "Edit PDF", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InputFileSection(
                label = "PDF to Edit",
                fileName = null,
                onPickFile = onPickFile
            )
            Text(
                "Draw colors over text to redact it, or use Text mode to replace text using the original font.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Editor ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorContent(
    state: EditPdfUiState.Ready,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSetMode: (EditMode) -> Unit,
    onSetColor: (Color) -> Unit,
    onSetBrush: (Float) -> Unit,
    onStrokeAdded: (DrawnStroke) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onCanvasTapped: (Offset) -> Unit,
    onTextConfirmed: (String, Offset) -> Unit,
    onTextDismissed: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bmp = state.currentBitmap
                if (bmp != null) {
                    PdfEditCanvas(
                        bitmap = bmp,
                        strokes = state.currentPageStrokes,
                        textPlacements = state.currentPageTexts,
                        pageWidthPt = state.pageWidthPt,
                        editMode = state.editMode,
                        currentColor = state.currentColor,
                        brushWidthFraction = state.brushWidthFraction,
                        onStrokeAdded = onStrokeAdded,
                        onTap = onCanvasTapped,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            ControlsPanel(
                state = state,
                onSetMode = onSetMode,
                onSetColor = onSetColor,
                onSetBrush = onSetBrush,
                onUndo = onUndo,
                onClear = onClear
            )

            PageNavBar(
                currentPage = state.currentPageIndex + 1,
                totalPages = state.totalPages,
                onPrevious = onPreviousPage,
                onNext = onNextPage
            )
        }
    }

    if (state.pendingTextPosition != null) {
        TextPlacementDialog(
            onConfirm = { text -> onTextConfirmed(text, state.pendingTextPosition) },
            onDismiss = onTextDismissed
        )
    }
}

// ── Canvas composable ─────────────────────────────────────────────────────────

@Composable
private fun PdfEditCanvas(
    bitmap: Bitmap,
    strokes: List<DrawnStroke>,
    textPlacements: List<TextPlacement>,
    pageWidthPt: Float,
    editMode: EditMode,
    currentColor: Color,
    brushWidthFraction: Float,
    onStrokeAdded: (DrawnStroke) -> Unit,
    onTap: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    var canvasPxSize by remember { mutableStateOf(Size.Zero) }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RectangleShape)
    ) {
        // Layer 1: PDF page bitmap
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "PDF page",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Stroke drawing canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    canvasPxSize = Size(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInput(editMode, currentColor, brushWidthFraction) {
                    when (editMode) {
                        EditMode.DRAW -> detectDragGestures(
                            onDragStart = { offset ->
                                if (canvasPxSize.width > 0f) {
                                    currentPoints = listOf(
                                        Offset(
                                            offset.x / canvasPxSize.width,
                                            offset.y / canvasPxSize.height
                                        )
                                    )
                                }
                            },
                            onDrag = { change, _ ->
                                if (canvasPxSize.width > 0f) {
                                    currentPoints = currentPoints + Offset(
                                        change.position.x / canvasPxSize.width,
                                        change.position.y / canvasPxSize.height
                                    )
                                }
                            },
                            onDragEnd = {
                                if (currentPoints.size >= 2) {
                                    onStrokeAdded(
                                        DrawnStroke(currentPoints, currentColor, brushWidthFraction)
                                    )
                                }
                                currentPoints = emptyList()
                            },
                            onDragCancel = { currentPoints = emptyList() }
                        )
                        EditMode.TEXT -> detectTapGestures { offset ->
                            if (canvasPxSize.width > 0f) {
                                onTap(
                                    Offset(
                                        offset.x / canvasPxSize.width,
                                        offset.y / canvasPxSize.height
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
            // Completed strokes
            strokes.forEach { stroke ->
                if (stroke.normalizedPoints.size >= 2) {
                    val path = Path()
                    stroke.normalizedPoints.forEachIndexed { i, p ->
                        val px = p.x * size.width
                        val py = p.y * size.height
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.widthFraction * size.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // In-progress stroke
            if (currentPoints.size >= 2) {
                val path = Path()
                currentPoints.forEachIndexed { i, p ->
                    val px = p.x * size.width
                    val py = p.y * size.height
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(
                    path = path,
                    color = currentColor,
                    style = Stroke(
                        width = brushWidthFraction * size.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Layer 3: Text placement previews (Compose Text composables, absolutely positioned)
        if (textPlacements.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()
                // font size: fontSize (PDF pt) relative to page width, scaled to canvas width
                val ptToPx = if (pageWidthPt > 0f) widthPx / pageWidthPt else 1f

                textPlacements.forEach { placement ->
                    val xDp = with(density) { (placement.normalizedX * widthPx).toDp() }
                    val yDp = with(density) { (placement.normalizedY * heightPx).toDp() }
                    val fontSizeSp = with(density) { (placement.fontSize * ptToPx).toSp() }

                    Text(
                        text = placement.text,
                        fontSize = fontSizeSp,
                        color = Color.Black,
                        modifier = Modifier.absoluteOffset(x = xDp, y = yDp)
                    )
                }
            }
        }
    }
}

// ── Controls panel ────────────────────────────────────────────────────────────

private val swatchColors = listOf(
    Color.White,
    Color.Black,
    Color(0xFFFFFF00),
    Color(0xFFFF5555)
)

@Composable
private fun ControlsPanel(
    state: EditPdfUiState.Ready,
    onSetMode: (EditMode) -> Unit,
    onSetColor: (Color) -> Unit,
    onSetBrush: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.editMode == EditMode.DRAW,
                onClick = { onSetMode(EditMode.DRAW) },
                label = { Text("Draw / Redact") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = state.editMode == EditMode.TEXT,
                onClick = { onSetMode(EditMode.TEXT) },
                label = { Text("Replace Text") },
                modifier = Modifier.weight(1f)
            )
        }

        if (state.editMode == EditMode.DRAW) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Color:", style = MaterialTheme.typography.labelMedium)
                swatchColors.forEach { color ->
                    ColorSwatch(
                        color = color,
                        selected = state.currentColor == color,
                        onClick = { onSetColor(color) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Brush:", style = MaterialTheme.typography.labelMedium)
                listOf("S" to 0.003f, "M" to 0.010f, "L" to 0.025f).forEach { (label, fraction) ->
                    FilterChip(
                        selected = kotlin.math.abs(state.brushWidthFraction - fraction) < 0.002f,
                        onClick = { onSetBrush(fraction) },
                        label = { Text(label) }
                    )
                }
            }
        } else {
            Text(
                "Tap anywhere on the page to place replacement text. Font is matched to the nearest original text.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = state.canUndo,
                modifier = Modifier.weight(1f)
            ) { Text("Undo") }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f)
            ) { Text("Clear Page") }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

// ── Page navigation bar ───────────────────────────────────────────────────────

@Composable
private fun PageNavBar(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = currentPage > 1) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
        }
        Text("Page $currentPage / $totalPages", style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onNext, enabled = currentPage < totalPages) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
        }
    }
}

// ── Text placement dialog ─────────────────────────────────────────────────────

@Composable
private fun TextPlacementDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Replacement Text") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("Place") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
