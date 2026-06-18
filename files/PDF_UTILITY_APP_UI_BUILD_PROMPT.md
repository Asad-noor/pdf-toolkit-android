# PDF Utility App - Complete UI Implementation Prompt

## Project Overview

You're building a comprehensive Jetpack Compose-based PDF utility application that provides offline PDF manipulation capabilities. The app has a robust backend with 14+ PDF operations and full clean architecture implementation (domain → data → UI layers).

**Current State**: The backend is **complete and fully tested**. The UI skeleton exists but only displays placeholder text. Your task is to build the **complete UI layer** that surfaces all PDF operations to users.

---

## Architecture Understanding

### Tech Stack
- **Framework**: Jetpack Compose (latest)
- **DI**: Hilt Android
- **Navigation**: Navigation3 (androidx.navigation3)
- **State Management**: ViewModel + StateFlow
- **Coroutines**: Kotlin Flow for async operations
- **Backend**: iText 7 for PDF processing
- **Testing**: JUnit + MockK

### Existing Layering
```
┌─────────────────────────────────────┐
│  UI Layer (TO BUILD)                │
│  ├─ Screens (Compose)               │
│  ├─ ViewModels                      │
│  └─ State Management                │
├─────────────────────────────────────┤
│  Domain Layer (COMPLETE)            │
│  ├─ Use Cases (14+ PDF operations)  │
│  ├─ Repositories (interfaces)       │
│  └─ Domain Models                   │
├─────────────────────────────────────┤
│  Data Layer (COMPLETE)              │
│  ├─ Repository Implementations      │
│  ├─ PDF Processors (iText + Android)│
│  └─ Data Sources                    │
└─────────────────────────────────────┘
```

---

## Core Domain Models

All these models exist and are ready to use:

### **PdfDocument** (represents a loaded PDF)
```kotlin
data class PdfDocument(
    val uri: String,
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val isPasswordProtected: Boolean,
    val author: String? = null,
    val title: String? = null,
    val creationDate: String? = null
)
```

### **ProcessingProgress** (tracks async operations)
```kotlin
data class ProcessingProgress(
    val current: Int,              // Current page/item
    val total: Int,                // Total to process
    val phase: Phase,              // READING, PROCESSING, WRITING, DONE
    val message: String? = null
) {
    val fraction: Float  // Progress 0f-1f for progress bars
    enum class Phase { READING, PROCESSING, WRITING, DONE }
}
```

### **PdfPageBitmap** (rendered page)
```kotlin
data class PdfPageBitmap(
    val pageIndex: Int,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int
)
```

### Other Key Models
- `CompressionLevel` (HIGH, MEDIUM, LOW)
- `RotationDegree` (ZERO, NINETY, ONE_EIGHTY, TWO_SEVENTY)
- `SplitMode` (BY_COUNT, BY_RANGE)
- `PasswordConfig(password: String, ownerPassword: String?)`
- `WatermarkConfig(text: String, opacity: Float, position: String)`
- `PdfError` (for error handling)
- `PdfOperationResult<T>` (Success/Failure wrapper)

---

## Available Use Cases (Injectable via Hilt)

All of these are ready to inject into ViewModels:

```kotlin
// Info Operations
GetPdfInfoUseCase          // Get metadata: pages, title, author, dates
RenderPageUseCase          // Render a single page as bitmap
ExtractTextUseCase         // Extract all text from PDF

// Manipulation Operations
MergePdfsUseCase           // Merge multiple PDFs into one
SplitPdfUseCase            // Split by page count or range
ExtractPagesUseCase        // Extract specific pages into new PDF
DeletePagesUseCase         // Remove pages from PDF
ReorderPagesUseCase        // Rearrange page order
RotatePageUseCase          // Rotate pages (90°, 180°, 270°)

// Enhancement Operations
CompressPdfUseCase         // Reduce file size
AddWatermarkUseCase        // Add text watermark
PasswordProtectUseCase     // Encrypt with password
UnlockPdfUseCase           // Decrypt password-protected PDF
ImagesToPdfUseCase         // Create PDF from images
```

All use cases follow this pattern:
```kotlin
class SomeUseCase @Inject constructor(
    private val pdfRepository: PdfRepository
) : FlowUseCase<SomeUseCase.Params>() {
    data class Params(/* input params */)
    override fun execute(params: Params): Flow<ProcessingProgress>
}
```

---

## Navigation Structure

The app uses Navigation3 with type-safe routing:

### Current Navigation Keys (defined in NavigationKeys.kt)
```kotlin
object Main : NavKey("Main")
// ADD MORE NAV KEYS HERE FOR:
// - PdfPickerScreen (select PDF from storage)
// - PdfDetailScreen (view PDF info, select operation)
// - MergePdfsScreen
// - SplitPdfScreen
// - RotatePageScreen
// - CompressScreen
// - PasswordScreen
// - WatermarkScreen
// - BatchOperationScreen
// etc.
```

Navigation is handled by:
- `rememberNavBackStack(initialKey)` to manage back stack
- `NavDisplay()` composable to render screens
- `entryProvider { }` DSL to map NavKeys to Composables

---

## UI Requirements (What to Build)

### 1. **Main Dashboard Screen**
**Location**: `MainActivity.kt` / `MainScreen.kt`

**Features**:
- Welcome/hero section with app description
- Grid/list of 14+ PDF operations as cards/buttons:
  - Merge PDFs
  - Split PDF
  - Extract Pages
  - Delete Pages
  - Reorder Pages
  - Rotate Pages
  - Compress
  - Add Watermark
  - Password Protect
  - Unlock PDF
  - Extract Text
  - Render Preview
  - Image to PDF
  - Get Info

- Quick access buttons: Recent files, Favorites
- File browser shortcut to open PDF

**State Management**:
```kotlin
class MainScreenViewModel : ViewModel() {
    val uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    // Define sealed class for state
}
```

---

### 2. **PDF Selection/Picker Screen**

**Features**:
- Browse device storage for PDFs
- Show PDF metadata (pages, size) in list
- Multi-select mode (for merge/batch operations)
- Filter by date, size, name
- Favorites/recent files section
- Document picker integration (for single file or SAF)

**Input Flow**:
- User navigates to operation → sees PdfPickerScreen
- Selects PDF(s) → returns selected Uri(s)
- User confirms → proceeds to operation screen

---

### 3. **PDF Detail / Preview Screen**

**Features**:
- Display PDF metadata (via GetPdfInfoUseCase):
  - Page count, file size
  - Title, Author, Creation date
  - Is password protected?
- PDF thumbnail/preview (render first page via RenderPageUseCase)
- Available operations for this PDF (context menu / bottom sheet)
- Share, duplicate, delete options

---

### 4. **Operation-Specific Screens**

#### **A. Merge PDFs Screen**
```
Input: List<Uri> selectedPdfs
- Show selected files in reorderable list
- Preview pages from each PDF
- Button: "Merge" → triggers MergePdfsUseCase
- ProgressBar tracking ProcessingProgress
Output: Merge result dialog + save location
```

#### **B. Split PDF Screen**
```
Input: Uri selectedPdf
- Two modes: "Split by count" or "Split by range"
  - By Count: Enter number of pages per split
  - By Range: Select pages to extract
- Preview: Show which pages go to which output
- Button: "Split" → triggers SplitPdfUseCase
Output: List of created PDFs
```

#### **C. Extract/Delete Pages Screen**
```
Input: Uri selectedPdf
- Show all pages (thumbnail grid or list)
- Multi-select pages to extract/delete
- Preview selection
- Button: "Extract"/"Delete" → trigger respective use cases
Output: New PDF or confirmation
```

#### **D. Reorder Pages Screen**
```
Input: Uri selectedPdf
- Draggable/reorderable list of page thumbnails
- Add, duplicate, remove, rotate pages
- Live preview
- Button: "Apply" → triggers ReorderPagesUseCase
Output: Modified PDF
```

#### **E. Rotate Pages Screen**
```
Input: Uri selectedPdf
- Grid view of pages
- Multi-select pages to rotate
- Rotation options: 90°, 180°, 270°
- Preview rotations in real-time
- Button: "Rotate" → triggers RotatePageUseCase
Output: Updated PDF
```

#### **F. Compress PDF Screen**
```
Input: Uri selectedPdf
- Display original file size
- Compression level selector: HIGH / MEDIUM / LOW
- Estimated output size preview
- Button: "Compress" → triggers CompressPdfUseCase
- Progress bar + "Estimated time remaining"
Output: Compressed PDF, show size reduction %
```

#### **G. Add Watermark Screen**
```
Input: Uri selectedPdf
- Text input for watermark text
- Position selector: TOP, CENTER, BOTTOM (or corners)
- Opacity slider: 0-100%
- Font size selector
- Color picker
- Preview (render sample page with watermark)
- Button: "Apply" → triggers AddWatermarkUseCase
Output: Watermarked PDF
```

#### **H. Password Protect Screen**
```
Input: Uri selectedPdf
- Password input field (masked)
- Confirm password field
- Owner password (optional, for restrictions)
- Encryption level selector (40-bit, 128-bit, 256-bit)
- Button: "Protect" → triggers PasswordProtectUseCase
Output: Password-protected PDF
```

#### **I. Unlock PDF Screen**
```
Input: Uri selectedPdf (detected as password-protected)
- Password input field
- Button: "Unlock" → triggers UnlockPdfUseCase
Output: Unlocked PDF
```

#### **J. Text Extraction Screen**
```
Input: Uri selectedPdf
- Button: "Extract All Text" → triggers ExtractTextUseCase
- Display extracted text in scrollable area
- Copy to clipboard button
- Save as .txt button
```

#### **K. Image to PDF Screen**
```
Input: List<Uri> selectedImages
- Show selected images in reorderable list
- Image preview
- Page size selector (A4, Letter, Custom)
- Orientation selector
- Button: "Create PDF" → triggers ImagesToPdfUseCase
Output: Created PDF
```

---

### 5. **Progress / Processing Screen**

**Shown during long-running operations**:
```
Features:
- Full-screen or dialog showing operation progress
- Progress bar (using ProcessingProgress.fraction)
- Current phase text: "Reading...", "Processing...", "Writing...", "Done!"
- Current page info: "Processing page 45 of 100"
- Cancel button (if operation supports cancellation)
- Time elapsed & estimated time remaining
- Operation log (phase transitions)
```

---

### 6. **Result / Success Screen**

**After operation completes**:
```
Features:
- Checkmark icon / success animation
- Operation summary: "Merged 3 PDFs into output.pdf"
- Result details:
  - Output file name
  - File size (before & after for compression)
  - Reduction % (if applicable)
- Buttons:
  - "Save to Downloads" (with file picker)
  - "Share" (via Android share sheet)
  - "View Result" (open in preview)
  - "New Operation" (return to dashboard)
```

---

### 7. **Error Handling Screen**

**If operation fails**:
```
Features:
- Error icon
- Error message: "Failed to merge PDFs: [reason]"
- Error details (expandable)
- Buttons:
  - "Retry"
  - "Back to Dashboard"
  - "Report Issue" (copy error to clipboard)
```

---

## Theme & Design

### Colors (Already Defined in theme/Color.kt)
- Primary, Secondary, Tertiary colors
- Surface, Background colors
- Error color

### Typography (Already Defined in theme/Type.kt)
- Display, Headline, Title, Body, Label styles
- Apply consistently

### Layout Principles
- **Safe Drawing Padding**: Respect system insets (status bar, nav bar)
- **Spacing**: Use 16.dp base unit for padding/margins
- **Cards**: Use Material3 Card with elevation for visual hierarchy
- **Dialogs**: Use AlertDialog for confirmations, ModalBottomSheet for heavy UI
- **Dark Mode**: Support both light/dark themes (MaterialTheme handles automatically)

---

## State Management Pattern

### ViewModel Template
```kotlin
sealed class OperationUiState {
    object Idle : OperationUiState()
    object Loading : OperationUiState()
    data class Progress(val progress: ProcessingProgress) : OperationUiState()
    data class Success(val result: String) : OperationUiState()
    data class Error(val message: String) : OperationUiState()
}

class OperationViewModel @Inject constructor(
    private val someUseCase: SomeUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val uiState = _uiState.asStateFlow()
    
    fun performOperation(params: SomeUseCase.Params) {
        viewModelScope.launch {
            _uiState.value = OperationUiState.Loading
            try {
                someUseCase.execute(params)
                    .collect { progress ->
                        _uiState.value = OperationUiState.Progress(progress)
                        if (progress.phase == ProcessingProgress.Phase.DONE) {
                            _uiState.value = OperationUiState.Success("Operation completed")
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = OperationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

---

## File Selection & Storage Access

### Using SAF (Storage Access Framework)
```kotlin
// In ViewModel or Activity
val pdfUri = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri: Uri? ->
    if (uri != null) {
        // Process uri
    }
}

// Launch picker
pdfUri.launch(arrayOf("application/pdf"))
```

### Android Permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

---

## Testing Expectations

### UI Tests (Espresso)
- Test navigation between screens
- Test file picker integration
- Test progress display during operations
- Test error state rendering

### ViewModel Tests (JUnit + MockK)
- Mock use cases
- Test state transitions
- Test error handling

### Screenshot/Preview Tests
- Use @Preview composables for all major screens

---

## Implementation Checklist

### Phase 1: Navigation & Shell
- [ ] Create all NavKey objects in NavigationKeys.kt
- [ ] Add entryProvider entries to Navigation.kt
- [ ] Create screen composable stubs (empty Column() placeholders)
- [ ] Test back navigation between screens

### Phase 2: Main Dashboard
- [ ] Build MainScreen with operation grid/list
- [ ] Add operation card components
- [ ] Navigation to sub-screens on card click
- [ ] Add file picker shortcut

### Phase 3: File Picker & Detail
- [ ] Implement PDF picker screen with SAF
- [ ] Build PDF detail screen
- [ ] Display metadata via GetPdfInfoUseCase
- [ ] Implement page preview via RenderPageUseCase

### Phase 4: Core Operation Screens
- [ ] Merge PDFs screen (MVP: just show selected files, merge button)
- [ ] Split PDF screen
- [ ] Extract/Delete pages
- [ ] Reorder pages
- [ ] Rotate pages

### Phase 5: Enhancement Operations
- [ ] Compress screen
- [ ] Watermark screen
- [ ] Password protect screen
- [ ] Unlock screen
- [ ] Text extraction screen
- [ ] Image to PDF screen

### Phase 6: Progress & Results
- [ ] Implement ProcessingProgress display
- [ ] Build success/error screens
- [ ] Add save dialog
- [ ] Implement share functionality

### Phase 7: Polish & Testing
- [ ] Dark mode support (already via MaterialTheme)
- [ ] Accessibility (content descriptors, semantics)
- [ ] Error messages user-friendly
- [ ] Add preview @Composable for each screen
- [ ] Write UI tests

---

## Key Integration Points

### Hilt Injection
All ViewModels use Hilt. Example:
```kotlin
@HiltViewModel
class MergeViewModel @Inject constructor(
    private val mergeUseCase: MergePdfsUseCase
) : ViewModel() { ... }

// In Composable
val viewModel: MergeViewModel = viewModel()
```

### Coroutine Scope
Use `viewModelScope.launch` in ViewModels for lifecycle-safe async operations.

### State Collection
Use `collectAsStateWithLifecycle()` in Composables to collect StateFlow:
```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
when (state) {
    is Idle -> { ... }
    is Loading -> { ... }
    is Progress -> { ... }
    is Success -> { ... }
    is Error -> { ... }
}
```

---

## Performance Considerations

- **Lazy Rendering**: Use `LazyColumn` / `LazyGrid` for long lists (PDF pages, file lists)
- **Bitmap Caching**: Cache rendered page bitmaps to avoid re-rendering
- **Coroutines**: Offload heavy work to IO dispatcher
- **Recomposition**: Use `key()` for LazyColumn items to minimize recomposition
- **Memory**: Be careful with large bitmaps; consider thumbnail size < 512x512

---

## Accessibility

- Add content descriptors: `modifier.contentDescription("...")`
- Use semantic modifiers for buttons/clickables
- Ensure color contrast >= 4.5:1 for text
- Support keyboard navigation (Tab through UI)
- Test with TalkBack screen reader

---

## Resources for Implementation

- **Jetpack Compose Docs**: https://developer.android.com/jetpack/compose
- **Material3 Components**: https://developer.android.com/jetpack/androidx/releases/compose-material3
- **Navigation3 Docs**: Recent AndroidX release docs
- **Hilt Guide**: https://developer.android.com/training/dependency-injection/hilt-android
- **Project Tests**: Check `/test` directories for patterns

---

## Final Notes

- **The backend is production-ready**: Focus on beautiful, intuitive UI.
- **Every use case is tested**: You don't need to validate business logic; trust the layers.
- **Type safety**: Use sealed classes and data classes extensively.
- **Modular design**: Each operation should be isolated in its own screen & ViewModel.
- **Progressive disclosure**: Show simple UI first, advanced options in bottom sheets or expanded sections.

**Goal**: Build a polished, professional PDF utility app that makes all 14+ operations accessible and intuitive.
