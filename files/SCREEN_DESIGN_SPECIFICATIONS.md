# PDF Utility App - Screen Design Specifications & Component Library

## Part 1: Reusable UI Components

### 1. **PdfFileCard**
A reusable card for displaying PDF file information in lists/grids.

**Props**:
```kotlin
@Composable
fun PdfFileCard(
    document: PdfDocument,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showSelectionCheckbox: Boolean = false
)
```

**Design**:
```
┌─────────────────────────────────────┐
│ [✓] filename.pdf                    │  <- checkbox (if multi-select mode)
│     📄 12 pages • 2.5 MB            │
│     Modified: Dec 15, 2024          │
│     Author: John Doe                │
│                                     │
│  [⋯ more details on expand]         │
└─────────────────────────────────────┘
```

**Features**:
- Thumbnail placeholder or actual PDF preview
- Title + filename
- Metadata: page count, file size, modification date, author
- Selection checkbox (for multi-select operations)
- Long-press for context menu: "Delete", "Share", "Rename", "Add to favorites"
- Ripple effect on click

---

### 2. **ProgressIndicator**
Unified progress display for all operations.

**Props**:
```kotlin
@Composable
fun PdfOperationProgress(
    progress: ProcessingProgress,
    modifier: Modifier = Modifier,
    showTimeRemaining: Boolean = true
)
```

**Design**:
```
┌────────────────────────────────────────┐
│  Processing PDF... 45% Complete       │
│  ████████░░░░░░░░░░  45/100           │
│                                        │
│  Phase: PROCESSING                    │
│  Page 45 of 100                       │
│  Estimated time remaining: 12 seconds │
│                                        │
│  [Cancel Operation]                   │
└────────────────────────────────────────┘
```

**Features**:
- Linear or circular progress bar
- Phase display with icon (📖 Reading, ⚙ Processing, 💾 Writing, ✓ Done)
- Current/total indicator
- Time remaining estimate
- Cancel button (if cancellable)
- Smooth animation on progress update

---

### 3. **OperationCard**
Card button for dashboard operations.

**Props**:
```kotlin
@Composable
fun OperationCard(
    title: String,
    description: String,
    iconRes: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPremium: Boolean = false
)
```

**Design**:
```
┌──────────────────────┐
│         📄          │
│   Merge PDFs        │
│                     │
│ Combine multiple    │
│ PDFs into one      │
│      [Tap]         │
└──────────────────────┘
```

**Features**:
- Large, tappable area
- Icon + title + description
- Elevation/shadow for depth
- Ripple effect
- Optional premium badge
- Grid layout (2-3 columns)

---

### 4. **PdfPageThumbnail**
Thumbnail display for page previews.

**Props**:
```kotlin
@Composable
fun PdfPageThumbnail(
    pageIndex: Int,
    bitmap: Bitmap?,
    isSelected: Boolean = false,
    onSelect: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

**Design**:
```
┌─────────────┐
│  ┌──────┐  │
│  │      │  │
│  │ Page │  │
│  │  42  │  │
│  └──────┘  │
│  [✓]       │  <- checkbox if selectable
└─────────────┘
```

**Features**:
- Aspect ratio preserved (typical A4: 0.707)
- Page number overlay
- Selection checkbox (optional)
- Placeholder while loading
- Long-press for options: "Rotate", "Delete", "Duplicate"

---

### 5. **ReorderableList**
Draggable list for reordering pages.

**Props**:
```kotlin
@Composable
fun ReorderablePageList(
    pages: List<PdfPageBitmap>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
)
```

**Features**:
- Drag handle (⋮⋮) on left
- Page thumbnails
- Smooth drag animation
- Haptic feedback on drop
- Real-time preview updates

---

### 6. **SliderWithLabel**
Labeled slider for settings.

**Props**:
```kotlin
@Composable
fun SliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    modifier: Modifier = Modifier
)
```

**Example**: Watermark opacity slider

---

### 7. **InputField**
Custom text input with validation.

**Props**:
```kotlin
@Composable
fun ValidatedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
)
```

---

### 8. **ConfirmationDialog**
Reusable confirmation dialog.

**Props**:
```kotlin
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel"
)
```

---

## Part 2: Screen-by-Screen Design

### **SCREEN 1: Dashboard / Main Screen**

**Location**: `/ui/main/MainScreen.kt`, `/ui/main/MainScreenViewModel.kt`

**Navigation Entry**:
```kotlin
object Main : NavKey("Main")
```

**State Management**:
```kotlin
sealed class MainUiState {
    object Loading : MainUiState()
    data class Loaded(val recentFiles: List<PdfDocument>) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {
    val uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    // Load recent files on init
}
```

**Layout** (Bottom to Top):
```
┌─────────────────────────────────────┐ <- TopAppBar
│ 🔍 PDF Utility                      │    (with search icon, settings gear)
├─────────────────────────────────────┤
│                                     │
│  Welcome Back, User!                │ <- Greeting
│  Ready to process PDFs?             │
│                                     │
│ ╔════════════════════════════════╗ │
│ ║ QUICK ACCESS                   ║ │
│ ║ ╔════════╗ ╔════════╗         ║ │
│ ║ ║📁 Pick ║ ║📋Recent║         ║ │
│ ║ ║ File  ║ ║ Files  ║         ║ │
│ ║ ╚════════╝ ╚════════╝         ║ │
│ ╚════════════════════════════════╝ │
│                                     │
│  PDF OPERATIONS                     │ <- Subheading
│  ┌─────────────┐  ┌─────────────┐  │
│  │   📚 Merge  │  │   ✂ Split   │  │ <- Operation cards (2 columns)
│  │   Combine   │  │   Separate  │  │
│  └─────────────┘  └─────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  │
│  │  🔖Extract  │  │  🗑 Delete  │  │
│  │   Pages     │  │   Pages     │  │
│  └─────────────┘  └─────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  │
│  │  🔄Reorder  │  │  🔁 Rotate  │  │
│  │   Pages     │  │   Pages     │  │
│  └─────────────┘  └─────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  │
│  │  🗜Compress │  │  💧Watermark│  │
│  │    PDF      │  │    Add      │  │
│  └─────────────┘  └─────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  │
│  │  🔐Password │  │  🔓Unlock   │  │
│  │   Protect   │  │    PDF      │  │
│  └─────────────┘  └─────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  │
│  │  📝Extract  │  │  🖼 Image   │  │
│  │    Text     │  │   to PDF    │  │
│  └─────────────┘  └─────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  │
│  │  ℹ️ Info   │  │  👁 Preview │  │
│  │   Get PDF   │  │   Render    │  │
│  │   Metadata  │  │   Pages     │  │
│  └─────────────┘  └─────────────┘  │
│                                     │
│  RECENT FILES                       │ <- Recent files section
│  ┌───────────────────────────────┐  │
│  │ 📄 report.pdf                  │  │
│  │ 12 pages • 2.5 MB              │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ 📄 invoice.pdf                 │  │
│  │ 1 page • 450 KB                │  │
│  └───────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

**Interactions**:
- Tap operation card → navigate to operation screen
- Tap "Pick File" → show PDF picker
- Tap recent file → show PDF detail screen
- Swipe down → refresh recent files
- Long-press operation card → show "Info" tooltip

---

### **SCREEN 2: PDF Picker / File Browser**

**Location**: `/ui/picker/PdfPickerScreen.kt`, `/ui/picker/PdfPickerViewModel.kt`

**Navigation Entry**:
```kotlin
data class PdfPicker(val allowMultiple: Boolean = false) : NavKey("PdfPicker")
```

**State**:
```kotlin
sealed class PdfPickerUiState {
    data class Files(
        val files: List<PdfDocument>,
        val selectedUris: Set<Uri> = emptySet()
    ) : PdfPickerUiState()
    object Loading : PdfPickerUiState()
    data class Error(val message: String) : PdfPickerUiState()
}
```

**Layout**:
```
┌─────────────────────────────────────┐
│ ← PDF Picker                        │ <- TopAppBar with back arrow
├─────────────────────────────────────┤
│ 🔍 [Search PDFs...              ] │ <- Search field
│ [Filter: ▼ Name]  [Sort: ▼ Date]   │ <- Filter/sort dropdowns
├─────────────────────────────────────┤
│ 📁 All PDFs (24 files)              │
│                                     │
│ ┌───────────────────────────────┐   │
│ │ [✓] report.pdf               │   │ <- Multi-select mode (if enabled)
│ │ 12 pages • 2.5 MB            │   │
│ │ Modified: Dec 15, 2024       │   │
│ └───────────────────────────────┘   │
│                                     │
│ ┌───────────────────────────────┐   │
│ │ [ ] invoice.pdf              │   │
│ │ 1 page • 450 KB              │   │
│ │ Modified: Dec 10, 2024       │   │
│ └───────────────────────────────┘   │
│                                     │
│ ┌───────────────────────────────┐   │
│ │ [✓] guide.pdf                │   │
│ │ 85 pages • 18 MB             │   │
│ │ Modified: Nov 22, 2024       │   │
│ └───────────────────────────────┘   │
│                                     │
│ ... (scrollable list)               │
│                                     │
├─────────────────────────────────────┤
│ [  Cancel  ]  [  Confirm (2) ▶ ]   │ <- Action buttons (bottom sticky)
└─────────────────────────────────────┘
```

**Features**:
- Search by filename
- Filter by date range, size range, favorites
- Sort by name, date, size
- Multi-select mode (toggleable)
- Show file metadata (pages, size, date, author)
- Favorites ⭐ shortcut (swipe right or long-press menu)
- Disabled "Confirm" button if no files selected
- Show count of selected files: "Confirm (2)"

---

### **SCREEN 3: PDF Detail / Info Screen**

**Location**: `/ui/detail/PdfDetailScreen.kt`, `/ui/detail/PdfDetailViewModel.kt`

**Navigation Entry**:
```kotlin
data class PdfDetail(val documentUri: Uri) : NavKey("PdfDetail")
```

**State**:
```kotlin
sealed class PdfDetailUiState {
    object Loading : PdfDetailUiState()
    data class Loaded(
        val document: PdfDocument,
        val firstPageBitmap: Bitmap?
    ) : PdfDetailUiState()
    data class Error(val message: String) : PdfDetailUiState()
}

@HiltViewModel
class PdfDetailViewModel @Inject constructor(
    private val getPdfInfoUseCase: GetPdfInfoUseCase,
    private val renderPageUseCase: RenderPageUseCase
) : ViewModel() {
    // Load on init with params
}
```

**Layout**:
```
┌─────────────────────────────────────┐
│ ← PDF Info                      ⋯   │ <- TopAppBar (⋯ = more options)
├─────────────────────────────────────┤
│                                     │
│  ┌──────────────────────────────┐   │
│  │                              │   │
│  │   [PDF Preview - Page 1]     │   │ <- Thumbnail of first page
│  │                              │   │
│  └──────────────────────────────┘   │
│                                     │
│  report.pdf                         │ <- Filename
│  Protected with password ⛔        │ <- If protected
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  📊 METADATA                        │
│  ┌───────────────────────────────┐  │
│  │ Pages:          12             │  │
│  │ File Size:      2.5 MB         │  │
│  │ Created:        Dec 15, 2024   │  │
│  │ Modified:       Dec 20, 2024   │  │
│  │ Author:         John Doe       │  │
│  │ Title:          Annual Report  │  │
│  │ Subject:        Finance        │  │
│  └───────────────────────────────┘  │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  ⚡ QUICK ACTIONS                   │
│  ┌──────────────┐  ┌──────────────┐ │
│  │ 🔄 Rotate    │  │ 🔐 Unlock    │ │
│  └──────────────┘  └──────────────┘ │
│  ┌──────────────┐  ┌──────────────┐ │
│  │ ✂ Extract    │  │ 🔗 Merge     │ │
│  │   Pages      │  │   With Other │ │
│  └──────────────┘  └──────────────┘ │
│  ┌──────────────┐  ┌──────────────┐ │
│  │ 🗜 Compress  │  │ 💾 Duplicate │ │
│  └──────────────┘  └──────────────┘ │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  📋 ALL OPERATIONS                  │
│  ┌───────────────────────────────┐  │
│  │ > Add Watermark               │  │
│  │ > Password Protect            │  │
│  │ > Compress                    │  │
│  │ > Split                       │  │
│  │ > Delete Pages                │  │
│  │ > Extract Text                │  │
│  │ ... (more)                    │  │
│  └───────────────────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│ [Share]  [⭐Add to Favorites]  [🗑]│ <- Bottom action bar
└─────────────────────────────────────┘
```

**Features**:
- Load PDF info via GetPdfInfoUseCase
- Render first page preview via RenderPageUseCase
- Display all metadata
- Show password protection status
- Quick action buttons (most common ops)
- Full operation list (scrollable)
- More options menu: "Rename", "Duplicate", "Delete", "View in explorer"
- Share button (Android Intent)
- Favorites toggle

---

### **SCREEN 4: Merge PDFs Screen**

**Navigation Entry**:
```kotlin
object MergePdfs : NavKey("MergePdfs")
```

**Layout**:
```
┌─────────────────────────────────────┐
│ ← Merge PDFs                    ⋯   │
├─────────────────────────────────────┤
│                                     │
│  Select PDF files to merge:         │
│  [+ Add PDF]  [+ Add Folder]        │
│                                     │
│  Selected Files (3):                │
│  ┌───────────────────────────────┐  │
│  │ ⋮⋮ 📄 report.pdf             │  │ <- Reorderable (drag handle)
│  │    12 pages • 2.5 MB          │  │
│  │                               │  │
│  │ ⋮⋮ 📄 invoice.pdf            │  │
│  │    1 page • 450 KB            │  │
│  │                               │  │
│  │ ⋮⋮ 📄 guide.pdf              │  │
│  │    85 pages • 18 MB           │  │
│  └───────────────────────────────┘  │
│                                     │
│  [✓] Optimize file sizes            │ <- Checkbox
│  [✓] Preserve form fields           │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  Output: [merged.pdf          ] ⌛ │ <- Editable filename
│                                     │
│  Total Size: 21 MB                  │
│  Estimated Time: 15 seconds         │
│                                     │
├─────────────────────────────────────┤
│ [  Cancel  ]  [  Merge ▶ ]         │
└─────────────────────────────────────┘
```

**State**:
```kotlin
sealed class MergeUiState {
    object Idle : MergeUiState()
    data class FilesSelected(val files: List<PdfDocument>) : MergeUiState()
    data class Processing(val progress: ProcessingProgress) : MergeUiState()
    data class Success(val outputUri: Uri, val outputSize: Long) : MergeUiState()
    data class Error(val message: String) : MergeUiState()
}

@HiltViewModel
class MergeViewModel @Inject constructor(
    private val mergeUseCase: MergePdfsUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<MergeUiState>(MergeUiState.Idle)
    val uiState = _uiState.asStateFlow()
    
    fun performMerge(inputUris: List<Uri>, outputFileName: String) {
        // Validate, call use case, handle progress/result
    }
}
```

**Features**:
- Add PDFs via file picker (multi-select)
- Reorderable list (drag handle)
- Remove individual PDFs (swipe or X button)
- Edit output filename
- Checkboxes for optimization options
- Display total size of output
- Estimate processing time
- Call MergePdfsUseCase on button press
- Navigate to ProcessingScreen while merge runs
- On success → navigate to ResultScreen

---

### **SCREEN 5: Processing / Progress Screen**

**Location**: `/ui/common/ProcessingScreen.kt`

**Navigation Entry**:
```kotlin
data class Processing(
    val operationName: String,
    val operationId: String
) : NavKey("Processing")
```

**Layout**:
```
┌─────────────────────────────────────┐
│           Merging PDFs...           │ <- Centered title
├─────────────────────────────────────┤
│                                     │
│           ⚙️  (spinner)            │ <- Loading spinner
│                                     │
│  ████████░░░░░░░░░░░░░░░░░░░  45% │ <- Progress bar
│                                     │
│  Phase: PROCESSING ⚙️              │ <- Phase indicator
│  Processing page 45 of 100         │
│                                     │
│  Estimated time remaining: 12 sec  │
│  Time elapsed: 3 seconds           │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  OPERATION LOG:                     │ <- Expandable log
│  ✓ 15:23:45 - Reading input files  │
│  ✓ 15:23:48 - Loading PDF 1/3      │
│  ✓ 15:23:50 - Loading PDF 2/3      │
│  ⊙ 15:23:52 - Processing...        │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
├─────────────────────────────────────┤
│        [Cancel Operation]           │
└─────────────────────────────────────┘
```

**Features**:
- Large, clear title
- Spinner or animated progress bar
- Current phase with icon
- Current/total counters
- Time remaining estimate
- Time elapsed
- Operation log (expandable)
- Cancel button (enables cancellation)
- Prevents back navigation (show confirmation)

---

### **SCREEN 6: Result / Success Screen**

**Location**: `/ui/common/ResultScreen.kt`

**Layout**:
```
┌─────────────────────────────────────┐
│           ✓ Success!                │ <- Checkmark or animation
├─────────────────────────────────────┤
│                                     │
│  Merged 3 PDFs successfully         │ <- Operation summary
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  📋 RESULT SUMMARY                  │
│  ┌───────────────────────────────┐  │
│  │ Output File: merged.pdf        │  │
│  │ Size: 21.0 MB                  │  │
│  │ Pages: 98 (12 + 1 + 85)        │  │
│  │ Processing Time: 15 seconds    │  │
│  └───────────────────────────────┘  │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  💾 SAVE RESULT                     │
│  [Save to Downloads]                │
│  [Save to Custom Location]          │
│  [Copy File Path]                   │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  ⚡ QUICK ACTIONS                   │
│  [👁 View]  [📤 Share]  [✂ More]  │
│                                     │
├─────────────────────────────────────┤
│[New Operation]  [Back to Dashboard] │
└─────────────────────────────────────┘
```

**Features**:
- Success animation (checkmark, confetti optional)
- Operation summary text
- Result details (output file, size, metrics)
- Save options (Downloads, custom location, copy path)
- View result (open in PDF viewer)
- Share result (Android Intent)
- More options (duplicate, rename, add to favorites)
- Navigation options

---

### **SCREEN 7: Error / Failure Screen**

**Layout**:
```
┌─────────────────────────────────────┐
│           ✗ Failed                  │ <- X or error animation
├─────────────────────────────────────┤
│                                     │
│  Failed to merge PDFs              │ <- Operation summary
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  ⚠️ ERROR DETAILS                   │
│  ┌───────────────────────────────┐  │
│  │ Error: Insufficient disk space│  │ <- Expandable error message
│  │                               │  │
│  │ [Show Full Details]           │  │
│  │ Failed at phase: WRITING      │  │
│  │ Time spent: 8 seconds         │  │
│  └───────────────────────────────┘  │
│                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│  💡 SUGGESTIONS                     │
│  • Free up disk space               │
│  • Check file permissions           │
│  • Try with fewer files             │
│                                     │
│  [📋 Copy Error to Clipboard]       │
│                                     │
├─────────────────────────────────────┤
│  [Retry]  [Back to Dashboard]       │
└─────────────────────────────────────┘
```

**Features**:
- Error icon/animation
- Brief error message
- Full error details (expandable)
- Error phase and time spent
- Suggestions for resolution
- Copy error to clipboard (for support)
- Retry button
- Back button

---

## Part 3: Component Implementation Patterns

### Pattern: Using ReorderableList with LazyColumn

```kotlin
@Composable
fun ReorderablePageListComposable(
    pages: List<PdfPageBitmap>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val reorderableState = rememberReorderableState(onMove = onReorder)
    
    LazyColumn(
        modifier = Modifier
            .reorderable(reorderableState)
            .detectReorderAfterLongPress(reorderableState)
    ) {
        items(
            items = pages,
            key = { it.pageIndex }
        ) { page ->
            ReorderableItem(
                reorderableState = reorderableState,
                key = page.pageIndex
            ) { isDragging ->
                PdfPageThumbnail(
                    pageIndex = page.pageIndex,
                    bitmap = page.bitmap,
                    modifier = Modifier
                        .alpha(if (isDragging) 0.5f else 1f)
                )
            }
        }
    }
}
```

### Pattern: Progress Bar with Phase Indicator

```kotlin
@Composable
fun PdfOperationProgress(progress: ProcessingProgress) {
    Column {
        Text("${progress.phase.name}: ${progress.message ?: ""}")
        
        LinearProgressIndicator(
            progress = progress.fraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        
        Text("${progress.current} / ${progress.total}")
    }
}
```

### Pattern: File Picker Integration

```kotlin
val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    viewModel.onFilesSelected(uris)
}

Button(onClick = {
    pdfPickerLauncher.launch(arrayOf("application/pdf"))
}) {
    Text("Select PDFs")
}
```

### Pattern: StateFlow Collection in Composable

```kotlin
@Composable
fun MyOperationScreen(viewModel: MyViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when (uiState) {
        is Idle -> IdleContent(onStart = { viewModel.start() })
        is Loading -> LoadingContent()
        is Progress -> ProgressContent(progress = (uiState as Progress).progress)
        is Success -> SuccessContent(result = (uiState as Success).result)
        is Error -> ErrorContent(error = (uiState as Error).message)
    }
}
```

---

## Part 4: Common Patterns & Best Practices

### Error Handling Pattern
```kotlin
// In ViewModel
fun performOperation() {
    viewModelScope.launch {
        try {
            _uiState.value = UiState.Loading
            useCase.execute(params)
                .collect { progress ->
                    _uiState.value = UiState.Progress(progress)
                    if (progress.phase == ProcessingProgress.Phase.DONE) {
                        _uiState.value = UiState.Success(...)
                    }
                }
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

### Permission Handling (if needed)
```kotlin
// For read/write storage (Android 6+)
val requestPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // Proceed with file operations
    }
}
```

### Safe File Operations
```kotlin
// Always use Content Resolver for Uri operations
val contentResolver = LocalContext.current.contentResolver
val inputStream = contentResolver.openInputStream(uri)
val outputStream = contentResolver.openOutputStream(outputUri)
```

---

This comprehensive design specification should guide your UI implementation while allowing flexibility in visual polish and animations.
