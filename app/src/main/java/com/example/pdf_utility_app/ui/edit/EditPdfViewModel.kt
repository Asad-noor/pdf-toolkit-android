package com.example.pdf_utility_app.ui.edit

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlinepdf.toolkit.core.domain.model.PageEdit
import com.offlinepdf.toolkit.core.domain.model.PdfPage
import com.offlinepdf.toolkit.core.domain.model.ProcessingProgress
import com.offlinepdf.toolkit.core.domain.model.TextRegion
import com.offlinepdf.toolkit.core.domain.repository.PdfRepository
import com.offlinepdf.toolkit.core.domain.usecase.pdf.ExtractPdfTextWithFontUseCase
import com.offlinepdf.toolkit.core.domain.usecase.pdf.SaveEditedPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject

// ── Domain models ─────────────────────────────────────────────────────────────

enum class EditMode { DRAW, HIGHLIGHT, ERASER, TEXT }

data class DrawnStroke(
    val normalizedPoints: List<Offset>,   // 0..1 within page, top-left origin
    val color: Color,
    val widthFraction: Float,             // fraction of page width
    val opacity: Float = 1f,             // 0..1 – used for highlight transparency
    val isEraser: Boolean = false         // true → clears overlay pixels (PorterDuff CLEAR)
)

data class TextPlacement(
    val normalizedX: Float,
    val normalizedY: Float,
    val text: String,
    val fontName: String,
    val fontSize: Float,
    val isBold: Boolean,
    val isItalic: Boolean
)

// redoStack holds DrawnStroke | TextPlacement items popped by undo
data class PageDrawingState(
    val strokes: List<DrawnStroke> = emptyList(),
    val texts: List<TextPlacement> = emptyList(),
    val redoStack: List<Any> = emptyList()
)

// ── UI state ──────────────────────────────────────────────────────────────────

sealed class EditPdfUiState {
    object Idle : EditPdfUiState()
    data class Loading(val message: String = "Opening PDF…") : EditPdfUiState()
    data class Ready(
        val pdfUri: Uri,
        val totalPages: Int,
        val currentPageIndex: Int,
        val currentBitmap: Bitmap?,
        val textRegionsForPage: List<TextRegion>,
        val pageWidthPt: Float,
        val pageHeightPt: Float,
        val editMode: EditMode,
        val currentColor: Color,
        val brushWidthFraction: Float,
        val brushOpacity: Float,
        val currentPageStrokes: List<DrawnStroke>,
        val currentPageTexts: List<TextPlacement>,
        val canUndo: Boolean,
        val canRedo: Boolean,
        val pendingTextPosition: Offset?
    ) : EditPdfUiState()
    data class Saving(val progress: ProcessingProgress) : EditPdfUiState()
    data class Success(val outputUri: Uri) : EditPdfUiState()
    data class Error(val message: String) : EditPdfUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class EditPdfViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val extractTextWithFontUseCase: ExtractPdfTextWithFontUseCase,
    private val saveEditedPdfUseCase: SaveEditedPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditPdfUiState>(EditPdfUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var pdfUri: Uri? = null
    private var allPages: List<PdfPage> = emptyList()
    private var allTextRegions: List<TextRegion> = emptyList()
    private val pageDrawingStates = mutableMapOf<Int, PageDrawingState>()
    private var currentPageIndex: Int = 0
    private var currentBitmap: Bitmap? = null
    private var editMode: EditMode = EditMode.DRAW
    private var currentColor: Color = Color.Black
    private var brushWidthFraction: Float = 0.010f
    private var brushOpacity: Float = 1f
    private var pendingTextPosition: Offset? = null
    private var saveJob: Job? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    fun initializeEditor(uri: Uri) {
        if (pdfUri == uri) return
        resetFields()
        pdfUri = uri
        viewModelScope.launch {
            _uiState.value = EditPdfUiState.Loading()
            val pagesResult = pdfRepository.getPageList(uri)
            if (pagesResult.isFailure) {
                _uiState.value = EditPdfUiState.Error(
                    "Failed to open PDF: ${pagesResult.exceptionOrNull()?.message}"
                )
                return@launch
            }
            allPages = pagesResult.getOrThrow()
            launch {
                allTextRegions = extractTextWithFontUseCase(
                    ExtractPdfTextWithFontUseCase.Params(uri)
                ).getOrElse { emptyList() }
                if (_uiState.value is EditPdfUiState.Ready) pushState()
            }
            loadPageBitmap(0)
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun navigateToPreviousPage() {
        if (currentPageIndex > 0) loadPageBitmap(currentPageIndex - 1)
    }

    fun navigateToNextPage() {
        if (currentPageIndex < allPages.size - 1) loadPageBitmap(currentPageIndex + 1)
    }

    private fun loadPageBitmap(pageIndex: Int) {
        val uri = pdfUri ?: return
        currentPageIndex = pageIndex
        currentBitmap = null
        if (_uiState.value is EditPdfUiState.Ready) pushState()
        viewModelScope.launch {
            val bitmap = pdfRepository.renderPageBitmap(uri, pageIndex, 150).getOrNull()
            currentBitmap = bitmap
            pushState()
        }
    }

    // ── Tool settings ─────────────────────────────────────────────────────────

    fun setEditMode(mode: EditMode) {
        editMode = mode
        when (mode) {
            EditMode.HIGHLIGHT -> {
                currentColor = Color(0xFFFFEB3B)
                brushWidthFraction = 0.030f
                brushOpacity = 0.4f
            }
            EditMode.ERASER -> {
                brushWidthFraction = 0.025f
                brushOpacity = 1f
            }
            EditMode.DRAW -> {
                // reset to opaque if coming from highlight
                if (brushOpacity < 1f) {
                    currentColor = Color.Black
                    brushOpacity = 1f
                    brushWidthFraction = 0.010f
                }
            }
            EditMode.TEXT -> {}
        }
        pushState()
    }

    fun setColor(color: Color) {
        currentColor = color
        pushState()
    }

    fun setBrushWidth(fraction: Float) {
        brushWidthFraction = fraction.coerceIn(0.002f, 0.06f)
        pushState()
    }

    fun setBrushOpacity(opacity: Float) {
        brushOpacity = opacity.coerceIn(0.05f, 1f)
        pushState()
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    fun addStroke(stroke: DrawnStroke) {
        val state = pageDrawingStates.getOrPut(currentPageIndex) { PageDrawingState() }
        pageDrawingStates[currentPageIndex] = state.copy(
            strokes = state.strokes + stroke,
            redoStack = emptyList()
        )
        pushState()
    }

    fun undo() {
        val state = pageDrawingStates[currentPageIndex] ?: return
        when {
            state.strokes.isNotEmpty() -> {
                val last = state.strokes.last()
                pageDrawingStates[currentPageIndex] = state.copy(
                    strokes = state.strokes.dropLast(1),
                    redoStack = state.redoStack + last
                )
            }
            state.texts.isNotEmpty() -> {
                val last = state.texts.last()
                pageDrawingStates[currentPageIndex] = state.copy(
                    texts = state.texts.dropLast(1),
                    redoStack = state.redoStack + last
                )
            }
            else -> return
        }
        pushState()
    }

    fun redo() {
        val state = pageDrawingStates[currentPageIndex] ?: return
        if (state.redoStack.isEmpty()) return
        val item = state.redoStack.last()
        pageDrawingStates[currentPageIndex] = when (item) {
            is DrawnStroke -> state.copy(
                strokes = state.strokes + item,
                redoStack = state.redoStack.dropLast(1)
            )
            is TextPlacement -> state.copy(
                texts = state.texts + item,
                redoStack = state.redoStack.dropLast(1)
            )
            else -> state
        }
        pushState()
    }

    fun clearPage() {
        pageDrawingStates[currentPageIndex] = PageDrawingState()
        pushState()
    }

    // ── Text placement ────────────────────────────────────────────────────────

    fun onCanvasTapped(normalizedPosition: Offset) {
        if (editMode != EditMode.TEXT) return
        pendingTextPosition = normalizedPosition
        pushState()
    }

    fun dismissTextDialog() {
        pendingTextPosition = null
        pushState()
    }

    fun confirmTextPlacement(text: String, position: Offset) {
        pendingTextPosition = null
        if (text.isBlank()) { pushState(); return }
        val page = allPages.getOrNull(currentPageIndex)
        val region = findNearestRegion(
            position.x, position.y,
            allTextRegions.filter { it.pageIndex == currentPageIndex },
            page?.widthPt ?: 595f,
            page?.heightPt ?: 842f
        )
        val placement = TextPlacement(
            normalizedX = position.x,
            normalizedY = position.y,
            text = text,
            fontName = region?.fontName ?: "Helvetica",
            fontSize = region?.fontSize ?: 12f,
            isBold = region?.isBold ?: false,
            isItalic = region?.isItalic ?: false
        )
        val state = pageDrawingStates.getOrPut(currentPageIndex) { PageDrawingState() }
        pageDrawingStates[currentPageIndex] = state.copy(
            texts = state.texts + placement,
            redoStack = emptyList()
        )
        pushState()
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveEdits(outputUri: Uri) {
        val uri = pdfUri ?: return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _uiState.value = EditPdfUiState.Saving(
                ProcessingProgress(0, 1, ProcessingProgress.Phase.PROCESSING)
            )
            val pageEdits = buildPageEdits()
            if (pageEdits.isEmpty()) {
                _uiState.value = EditPdfUiState.Success(outputUri)
                return@launch
            }
            saveEditedPdfUseCase(SaveEditedPdfUseCase.Params(uri, pageEdits, outputUri))
                .collect { progress ->
                    _uiState.value = if (progress.phase == ProcessingProgress.Phase.DONE) {
                        EditPdfUiState.Success(outputUri)
                    } else {
                        EditPdfUiState.Saving(progress)
                    }
                }
        }
    }

    fun reset() {
        resetFields()
        _uiState.value = EditPdfUiState.Idle
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun pushState() {
        val uri = pdfUri ?: return
        val idx = currentPageIndex
        val page = allPages.getOrNull(idx)
        val drawState = pageDrawingStates[idx] ?: PageDrawingState()
        _uiState.value = EditPdfUiState.Ready(
            pdfUri = uri,
            totalPages = allPages.size.coerceAtLeast(1),
            currentPageIndex = idx,
            currentBitmap = currentBitmap,
            textRegionsForPage = allTextRegions.filter { it.pageIndex == idx },
            pageWidthPt = page?.widthPt ?: 595f,
            pageHeightPt = page?.heightPt ?: 842f,
            editMode = editMode,
            currentColor = currentColor,
            brushWidthFraction = brushWidthFraction,
            brushOpacity = brushOpacity,
            currentPageStrokes = drawState.strokes,
            currentPageTexts = drawState.texts,
            canUndo = drawState.strokes.isNotEmpty() || drawState.texts.isNotEmpty(),
            canRedo = drawState.redoStack.isNotEmpty(),
            pendingTextPosition = pendingTextPosition
        )
    }

    private fun resetFields() {
        pdfUri = null
        allPages = emptyList()
        allTextRegions = emptyList()
        pageDrawingStates.clear()
        currentPageIndex = 0
        currentBitmap = null
        editMode = EditMode.DRAW
        currentColor = Color.Black
        brushWidthFraction = 0.010f
        brushOpacity = 1f
        pendingTextPosition = null
        saveJob?.cancel()
        saveJob = null
    }

    private fun buildPageEdits(): List<PageEdit> =
        pageDrawingStates.mapNotNull { (pageIndex, drawState) ->
            if (drawState.strokes.isEmpty() && drawState.texts.isEmpty()) return@mapNotNull null
            val page = allPages.getOrNull(pageIndex) ?: return@mapNotNull null
            PageEdit(pageIndex, createOverlayBitmap(drawState, page.widthPt, page.heightPt))
        }

    private fun createOverlayBitmap(
        state: PageDrawingState,
        pageWidthPt: Float,
        pageHeightPt: Float
    ): Bitmap {
        val renderDpi = 150f
        val width = (pageWidthPt * renderDpi / 72f).roundToInt().coerceAtLeast(1)
        val height = (pageHeightPt * renderDpi / 72f).roundToInt().coerceAtLeast(1)

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)

        state.strokes.forEach { stroke ->
            if (stroke.normalizedPoints.size < 2) return@forEach
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeWidth = stroke.widthFraction * width *
                    if (stroke.isEraser) 3f else 1f
                if (stroke.isEraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    color = stroke.color.copy(alpha = stroke.opacity).toArgb()
                }
            }
            canvas.drawPath(
                buildSmoothAndroidPath(stroke.normalizedPoints, width.toFloat(), height.toFloat()),
                paint
            )
        }

        state.texts.forEach { placement ->
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = placement.fontSize * renderDpi / 72f
                typeface = android.graphics.Typeface.create(
                    placement.fontName,
                    when {
                        placement.isBold && placement.isItalic -> android.graphics.Typeface.BOLD_ITALIC
                        placement.isBold -> android.graphics.Typeface.BOLD
                        placement.isItalic -> android.graphics.Typeface.ITALIC
                        else -> android.graphics.Typeface.NORMAL
                    }
                )
                isAntiAlias = true
            }
            canvas.drawText(
                placement.text,
                placement.normalizedX * width,
                placement.normalizedY * height,
                paint
            )
        }

        return bmp
    }

    private fun buildSmoothAndroidPath(
        points: List<Offset>,
        width: Float,
        height: Float
    ): android.graphics.Path {
        val path = android.graphics.Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x * width, points[0].y * height)
        if (points.size == 2) {
            path.lineTo(points[1].x * width, points[1].y * height)
            return path
        }
        for (i in 1 until points.size - 1) {
            val cx = points[i].x * width
            val cy = points[i].y * height
            val midX = (cx + points[i + 1].x * width) / 2f
            val midY = (cy + points[i + 1].y * height) / 2f
            path.quadTo(cx, cy, midX, midY)
        }
        path.lineTo(points.last().x * width, points.last().y * height)
        return path
    }

    private fun findNearestRegion(
        nx: Float, ny: Float,
        regions: List<TextRegion>,
        pageWidthPt: Float,
        pageHeightPt: Float
    ): TextRegion? {
        if (regions.isEmpty()) return null
        val tapX = nx * pageWidthPt
        val tapY = (1f - ny) * pageHeightPt
        return regions.minByOrNull { r ->
            val cx = (r.bounds.left + r.bounds.right) / 2f
            val cy = (r.bounds.top + r.bounds.bottom) / 2f
            val dx = cx - tapX; val dy = cy - tapY
            dx * dx + dy * dy
        }
    }
}
