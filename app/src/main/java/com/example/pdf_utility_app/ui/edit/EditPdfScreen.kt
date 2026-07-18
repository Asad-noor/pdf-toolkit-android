package com.example.pdf_utility_app.ui.edit

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdf_utility_app.ui.components.ErrorContent
import com.example.pdf_utility_app.ui.components.InputFileSection
import com.example.pdf_utility_app.ui.components.PdfTopAppBar
import com.example.pdf_utility_app.ui.components.ProgressContent
import com.example.pdf_utility_app.ui.components.SuccessContent

// ── Color palettes ────────────────────────────────────────────────────────────

private val drawColors = listOf(
    Color.Black,
    Color(0xFF424242),  // Dark gray
    Color(0xFFE53935),  // Red
    Color(0xFF1E88E5),  // Blue
    Color(0xFF43A047),  // Green
    Color(0xFFFB8C00),  // Orange
    Color(0xFF8E24AA),  // Purple
    Color.White
)

private val highlightColors = listOf(
    Color(0xFFFFEB3B),  // Yellow
    Color(0xFF69F0AE),  // Cyan-green
    Color(0xFFFF80AB),  // Pink
    Color(0xFF80D8FF),  // Light blue
    Color(0xFFFFCC80)   // Peach
)

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
            onSetOpacity = viewModel::setBrushOpacity,
            onStrokeAdded = viewModel::addStroke,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
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

// ── Idle ──────────────────────────────────────────────────────────────────────

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
                "Draw, highlight, erase, or add text directly on any page. " +
                    "Changes are stamped into the saved PDF.",
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
    onSetOpacity: (Float) -> Unit,
    onStrokeAdded: (DrawnStroke) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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
                title = {
                    Column {
                        Text(
                            text = "Edit PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Page ${state.currentPageIndex + 1} of ${state.totalPages}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
            // PDF canvas — dark background like Adobe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF2B2B2B)),
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
                        brushOpacity = state.brushOpacity,
                        onStrokeAdded = onStrokeAdded,
                        onTap = onCanvasTapped,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Editor controls panel
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                EditorPanel(
                    state = state,
                    onSetMode = onSetMode,
                    onSetColor = onSetColor,
                    onSetBrush = onSetBrush,
                    onSetOpacity = onSetOpacity,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onClear = onClear,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage
                )
            }
        }
    }

    if (state.pendingTextPosition != null) {
        TextPlacementDialog(
            onConfirm = { text -> onTextConfirmed(text, state.pendingTextPosition) },
            onDismiss = onTextDismissed
        )
    }
}

// ── PDF canvas with smooth curves + eraser ────────────────────────────────────

@Composable
private fun PdfEditCanvas(
    bitmap: Bitmap,
    strokes: List<DrawnStroke>,
    textPlacements: List<TextPlacement>,
    pageWidthPt: Float,
    editMode: EditMode,
    currentColor: Color,
    brushWidthFraction: Float,
    brushOpacity: Float,
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
        // Layer 1: PDF page
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "PDF page",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Drawing overlay — offscreen layer so BlendMode.Clear works for eraser
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .onSizeChanged { size ->
                    canvasPxSize = Size(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInput(editMode, currentColor, brushWidthFraction, brushOpacity) {
                    when (editMode) {
                        EditMode.DRAW, EditMode.HIGHLIGHT, EditMode.ERASER -> detectDragGestures(
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
                                    val normalized = Offset(
                                        change.position.x / canvasPxSize.width,
                                        change.position.y / canvasPxSize.height
                                    )
                                    // Skip duplicate points for performance
                                    val last = currentPoints.lastOrNull()
                                    if (last == null || (normalized - last).getDistance() > 0.002f) {
                                        currentPoints = currentPoints + normalized
                                    }
                                }
                            },
                            onDragEnd = {
                                if (currentPoints.size >= 2) {
                                    onStrokeAdded(
                                        DrawnStroke(
                                            normalizedPoints = currentPoints,
                                            color = currentColor,
                                            widthFraction = brushWidthFraction,
                                            opacity = brushOpacity,
                                            isEraser = editMode == EditMode.ERASER
                                        )
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
            // Render committed strokes
            strokes.forEach { stroke ->
                if (stroke.normalizedPoints.size >= 2) {
                    val path = buildSmoothPath(stroke.normalizedPoints, size)
                    if (stroke.isEraser) {
                        drawPath(
                            path = path,
                            color = Color.Transparent,
                            style = Stroke(
                                width = stroke.widthFraction * size.width * 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            ),
                            blendMode = BlendMode.Clear
                        )
                    } else {
                        drawPath(
                            path = path,
                            color = stroke.color.copy(alpha = stroke.opacity),
                            style = Stroke(
                                width = stroke.widthFraction * size.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Render in-progress stroke
            if (currentPoints.size >= 2) {
                val path = buildSmoothPath(currentPoints, size)
                val isErasing = editMode == EditMode.ERASER
                if (isErasing) {
                    drawPath(
                        path = path,
                        color = Color.Transparent,
                        style = Stroke(
                            width = brushWidthFraction * size.width * 3f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = BlendMode.Clear
                    )
                } else {
                    drawPath(
                        path = path,
                        color = currentColor.copy(alpha = brushOpacity),
                        style = Stroke(
                            width = brushWidthFraction * size.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Layer 3: Text placement previews
        if (textPlacements.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()
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

private fun buildSmoothPath(points: List<Offset>, size: Size): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x * size.width, points[0].y * size.height)
    if (points.size == 2) {
        path.lineTo(points[1].x * size.width, points[1].y * size.height)
        return path
    }
    for (i in 1 until points.size - 1) {
        val cx = points[i].x * size.width
        val cy = points[i].y * size.height
        val midX = (cx + points[i + 1].x * size.width) / 2f
        val midY = (cy + points[i + 1].y * size.height) / 2f
        path.quadraticTo(cx, cy, midX, midY)
    }
    path.lineTo(points.last().x * size.width, points.last().y * size.height)
    return path
}

// ── Editor panel ──────────────────────────────────────────────────────────────

@Composable
private fun EditorPanel(
    state: EditPdfUiState.Ready,
    onSetMode: (EditMode) -> Unit,
    onSetColor: (Color) -> Unit,
    onSetBrush: (Float) -> Unit,
    onSetOpacity: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        // Tool selector + history row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ToolButton(
                    imageVector = Icons.Default.Edit,
                    label = "Draw",
                    selected = state.editMode == EditMode.DRAW,
                    onClick = { onSetMode(EditMode.DRAW) }
                )
                ToolButton(
                    imageVector = Icons.Default.Brush,
                    label = "Highlight",
                    selected = state.editMode == EditMode.HIGHLIGHT,
                    onClick = { onSetMode(EditMode.HIGHLIGHT) }
                )
                ToolButton(
                    imageVector = Icons.Default.Clear,
                    label = "Erase",
                    selected = state.editMode == EditMode.ERASER,
                    onClick = { onSetMode(EditMode.ERASER) }
                )
                ToolButton(
                    imageVector = Icons.Default.Title,
                    label = "Text",
                    selected = state.editMode == EditMode.TEXT,
                    onClick = { onSetMode(EditMode.TEXT) }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onUndo, enabled = state.canUndo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (state.canUndo) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(onClick = onRedo, enabled = state.canRedo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.canRedo) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear page",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        // Contextual tool options
        AnimatedContent(
            targetState = state.editMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tool_options"
        ) { mode ->
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                when (mode) {
                    EditMode.DRAW -> DrawOptions(
                        currentColor = state.currentColor,
                        brushWidthFraction = state.brushWidthFraction,
                        onSetColor = onSetColor,
                        onSetBrush = onSetBrush
                    )
                    EditMode.HIGHLIGHT -> HighlightOptions(
                        currentColor = state.currentColor,
                        brushWidthFraction = state.brushWidthFraction,
                        brushOpacity = state.brushOpacity,
                        onSetColor = onSetColor,
                        onSetBrush = onSetBrush,
                        onSetOpacity = onSetOpacity
                    )
                    EditMode.ERASER -> EraserOptions(
                        brushWidthFraction = state.brushWidthFraction,
                        onSetBrush = onSetBrush
                    )
                    EditMode.TEXT -> TextModeHint()
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        // Page navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousPage,
                enabled = state.currentPageIndex > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
            }
            Text(
                text = "Page ${state.currentPageIndex + 1} / ${state.totalPages}",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onNextPage,
                enabled = state.currentPageIndex < state.totalPages - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
            }
        }
    }
}

// ── Tool button ───────────────────────────────────────────────────────────────

@Composable
private fun ToolButton(
    imageVector: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val selectedBg = MaterialTheme.colorScheme.secondaryContainer
    val iconTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) selectedBg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconTint
        )
    }
}

// ── Draw options ──────────────────────────────────────────────────────────────

@Composable
private fun DrawOptions(
    currentColor: Color,
    brushWidthFraction: Float,
    onSetColor: (Color) -> Unit,
    onSetBrush: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(42.dp)
            )
            drawColors.forEach { color ->
                ColorSwatch(
                    color = color,
                    selected = currentColor == color,
                    onClick = { onSetColor(color) }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Size",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value = brushWidthFraction,
                onValueChange = onSetBrush,
                valueRange = 0.002f..0.05f,
                modifier = Modifier.weight(1f)
            )
            BrushPreviewDot(color = currentColor, sizeFraction = brushWidthFraction)
        }
    }
}

// ── Highlight options ─────────────────────────────────────────────────────────

@Composable
private fun HighlightOptions(
    currentColor: Color,
    brushWidthFraction: Float,
    brushOpacity: Float,
    onSetColor: (Color) -> Unit,
    onSetBrush: (Float) -> Unit,
    onSetOpacity: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(42.dp)
            )
            highlightColors.forEach { color ->
                ColorSwatch(
                    color = color.copy(alpha = 0.75f),
                    selected = currentColor == color || currentColor == color.copy(alpha = 0.75f),
                    onClick = { onSetColor(color) }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Width",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value = brushWidthFraction,
                onValueChange = onSetBrush,
                valueRange = 0.010f..0.07f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Opacity",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value = brushOpacity,
                onValueChange = onSetOpacity,
                valueRange = 0.1f..0.9f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(brushOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Eraser options ────────────────────────────────────────────────────────────

@Composable
private fun EraserOptions(
    brushWidthFraction: Float,
    onSetBrush: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Size",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp)
        )
        Slider(
            value = brushWidthFraction,
            onValueChange = onSetBrush,
            valueRange = 0.010f..0.08f,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Text mode hint ────────────────────────────────────────────────────────────

@Composable
private fun TextModeHint() {
    Text(
        text = "Tap anywhere on the page to place text. " +
            "The font size and style will match the nearest existing text.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.height(48.dp)
    )
}

// ── Color swatch ──────────────────────────────────────────────────────────────

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.4f) Color.Black else Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Brush size preview dot ────────────────────────────────────────────────────

@Composable
private fun BrushPreviewDot(color: Color, sizeFraction: Float) {
    val clampedRadius = (sizeFraction * 200f).coerceIn(3f, 20f)
    Canvas(modifier = Modifier.size(32.dp)) {
        drawCircle(color = color, radius = clampedRadius)
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
        title = { Text("Add Text") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Place")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
