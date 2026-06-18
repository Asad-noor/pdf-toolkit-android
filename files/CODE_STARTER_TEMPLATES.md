# PDF Utility App - Code Starter Templates & Snippets

## Quick Start Navigation Setup

### Step 1: Define All Navigation Keys

**File**: `NavigationKeys.kt`

```kotlin
package com.example.pdf_utility_app

import androidx.navigation3.runtime.NavKey

// Main entry
object Main : NavKey("Main")

// Picker & Detail
data class PdfPicker(val allowMultiple: Boolean = false) : NavKey("PdfPicker")
data class PdfDetail(val documentUri: String) : NavKey("PdfDetail")

// Operations
object MergePdfs : NavKey("MergePdfs")
object SplitPdf : NavKey("SplitPdf")
object ExtractPages : NavKey("ExtractPages")
object DeletePages : NavKey("DeletePages")
object ReorderPages : NavKey("ReorderPages")
object RotatePages : NavKey("RotatePages")
object CompressPdf : NavKey("CompressPdf")
object AddWatermark : NavKey("AddWatermark")
object PasswordProtect : NavKey("PasswordProtect")
object UnlockPdf : NavKey("UnlockPdf")
object ExtractText : NavKey("ExtractText")
object ImagesToPdf : NavKey("ImagesToPdf")
object GetPdfInfo : NavKey("GetPdfInfo")

// Common
data class Processing(
    val operationName: String,
    val operationId: String = System.currentTimeMillis().toString()
) : NavKey("Processing")

data class Result(
    val operationId: String,
    val success: Boolean,
    val message: String? = null
) : NavKey("Result")
```

---

## Basic ViewModel Template

### State Definition
```kotlin
// src/main/java/com/example/pdf_utility_app/ui/merge/MergeViewModel.kt

sealed class MergeUiState {
    object Idle : MergeUiState()
    data class FilesSelected(
        val files: List<PdfDocument>,
        val outputFileName: String = "merged.pdf"
    ) : MergeUiState()
    data class Processing(val progress: ProcessingProgress) : MergeUiState()
    data class Success(
        val outputUri: Uri,
        val outputSize: Long,
        val pageCount: Int
    ) : MergeUiState()
    data class Error(val message: String, val exception: Exception? = null) : MergeUiState()
}

@HiltViewModel
class MergeViewModel @Inject constructor(
    private val mergeUseCase: MergePdfsUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MergeUiState>(MergeUiState.Idle)
    val uiState = _uiState.asStateFlow()
    
    private val _selectedFiles = MutableStateFlow<List<PdfDocument>>(emptyList())
    val selectedFiles = _selectedFiles.asStateFlow()
    
    fun onFilesSelected(documents: List<PdfDocument>) {
        _selectedFiles.value = documents
        _uiState.value = MergeUiState.FilesSelected(documents)
    }
    
    fun updateOutputFileName(fileName: String) {
        val currentState = _uiState.value
        if (currentState is MergeUiState.FilesSelected) {
            _uiState.value = currentState.copy(outputFileName = fileName)
        }
    }
    
    fun performMerge(outputFileName: String) {
        val inputUris = _selectedFiles.value.map { it.uri.toUri() }
        
        if (inputUris.isEmpty()) {
            _uiState.value = MergeUiState.Error("No files selected")
            return
        }
        
        viewModelScope.launch {
            try {
                val outputFile = fileRepository.createTempFile(outputFileName)
                val outputUri = outputFile.uri.toUri()
                
                mergeUseCase.execute(
                    MergePdfsUseCase.Params(inputUris, outputUri)
                ).collect { progress ->
                    _uiState.value = MergeUiState.Processing(progress)
                    
                    if (progress.phase == ProcessingProgress.Phase.DONE) {
                        val fileSize = outputFile.length()
                        _uiState.value = MergeUiState.Success(
                            outputUri,
                            fileSize,
                            pageCount = progress.total // Approximate
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MergeUiState.Error(
                    e.message ?: "Unknown error occurred",
                    e
                )
            }
        }
    }
}
```

---

## Basic Screen Template

```kotlin
// src/main/java/com/example/pdf_utility_app/ui/merge/MergeScreen.kt

@Composable
fun MergePdfsScreen(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MergeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
    
    when (uiState) {
        is MergeUiState.Idle -> {
            MergePdfsContent(
                selectedFiles = selectedFiles,
                onAddFiles = {
                    onNavigate(PdfPicker(allowMultiple = true))
                },
                onMerge = { outputFileName ->
                    viewModel.updateOutputFileName(outputFileName)
                    viewModel.performMerge(outputFileName)
                },
                onBack = onBack,
                modifier = modifier
            )
        }
        is MergeUiState.FilesSelected -> {
            MergePdfsContent(
                selectedFiles = (uiState as MergeUiState.FilesSelected).files,
                defaultOutputFileName = (uiState as MergeUiState.FilesSelected).outputFileName,
                onAddFiles = {
                    onNavigate(PdfPicker(allowMultiple = true))
                },
                onMerge = { outputFileName ->
                    viewModel.performMerge(outputFileName)
                },
                onBack = onBack,
                modifier = modifier
            )
        }
        is MergeUiState.Processing -> {
            ProcessingScreen(
                progress = (uiState as MergeUiState.Processing).progress,
                operationName = "Merge PDFs",
                onCancel = { /* handle cancellation */ }
            )
        }
        is MergeUiState.Success -> {
            val state = uiState as MergeUiState.Success
            ResultScreen(
                title = "PDFs Merged Successfully",
                message = "Merged ${selectedFiles.size} files",
                details = mapOf(
                    "Output File" to "merged.pdf",
                    "Total Size" to formatFileSize(state.outputSize),
                    "Pages" to state.pageCount.toString()
                ),
                onViewResult = {
                    // Open PDF viewer
                },
                onShare = {
                    // Share file
                },
                onNewOperation = {
                    onNavigate(Main)
                },
                onBack = onBack
            )
        }
        is MergeUiState.Error -> {
            ErrorScreen(
                message = (uiState as MergeUiState.Error).message,
                onRetry = { viewModel.performMerge("merged.pdf") },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun MergePdfsContent(
    selectedFiles: List<PdfDocument>,
    defaultOutputFileName: String = "merged.pdf",
    onAddFiles: () -> Unit,
    onMerge: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var outputFileName by remember { mutableStateOf(defaultOutputFileName) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top app bar
        TopAppBar(onBack = onBack, title = "Merge PDFs")
        
        // Content
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select PDF files to merge:", style = MaterialTheme.typography.titleMedium)
            
            Button(onClick = onAddFiles) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add PDFs")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (selectedFiles.isNotEmpty()) {
                Text("Selected Files (${selectedFiles.size}):", style = MaterialTheme.typography.labelMedium)
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(selectedFiles) { document ->
                        PdfFileCard(document = document, onClick = {})
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Output filename
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    label = { Text("Output Filename") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // File size info
                Text(
                    "Estimated output size: ${formatFileSize(selectedFiles.sumOf { it.fileSizeBytes })}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("No files selected. Tap 'Add PDFs' to get started.")
            }
        }
    }
    
    // Bottom action bar
    BottomActionBar(
        primaryButtonText = "Merge",
        primaryButtonEnabled = selectedFiles.isNotEmpty(),
        onPrimaryClick = { onMerge(outputFileName) },
        onSecondaryClick = onBack
    )
}
```

---

## File Picker Integration

```kotlin
// In your screen composable
val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    val documents = uris.map { uri ->
        // You'll need to load document info
        PdfDocument(
            uri = uri.toString(),
            fileName = uri.lastPathSegment ?: "Unknown",
            pageCount = 0,  // Load via GetPdfInfoUseCase
            fileSizeBytes = 0,  // Get from file
            isPasswordProtected = false
        )
    }
    viewModel.onFilesSelected(documents)
}

// Launch picker
pdfPickerLauncher.launch(arrayOf("application/pdf"))
```

---

## Common Composable Components

### TopAppBar with Back Button
```kotlin
@Composable
fun TopAppBar(
    onBack: () -> Unit,
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions
    )
}
```

### Bottom Action Bar
```kotlin
@Composable
fun BottomActionBar(
    primaryButtonText: String,
    primaryButtonEnabled: Boolean = true,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    secondaryButtonText: String = "Cancel",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSecondaryClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(secondaryButtonText)
            }
            
            Button(
                onClick = onPrimaryClick,
                enabled = primaryButtonEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(primaryButtonText)
            }
        }
    }
}
```

### Progress Indicator
```kotlin
@Composable
fun PdfOperationProgress(
    progress: ProcessingProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Processing PDF... ${(progress.fraction * 100).toInt()}%",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = progress.fraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "${progress.current} / ${progress.total}",
            style = MaterialTheme.typography.bodySmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Phase: ${progress.phase.name}",
            style = MaterialTheme.typography.labelMedium
        )
        
        if (progress.message != null) {
            Text(
                progress.message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

---

## File Size Formatting Utility

```kotlin
fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return String.format("%.2f %s", size, units[unitIndex])
}
```

---

## Reorderable List Setup

If using a library like `reorderable-lazycolumn`:

```gradle
// In build.gradle.kts (app level)
dependencies {
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
}
```

```kotlin
@Composable
fun ReorderablePageList(
    pages: List<PdfPageBitmap>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val reorderableState = rememberReorderableState(onMove = { from, to ->
        onReorder(from, to)
    })
    
    LazyColumn(
        modifier = modifier.reorderable(reorderableState),
        state = reorderableState.listState
    ) {
        items(pages, key = { it.pageIndex }) { page ->
            ReorderableItem(
                reorderableState,
                key = page.pageIndex
            ) { isDragging ->
                PdfPageThumbnail(
                    pageIndex = page.pageIndex,
                    bitmap = page.bitmap,
                    modifier = Modifier.alpha(if (isDragging) 0.5f else 1f)
                )
            }
        }
    }
}
```

---

## Activity Result Contracts for File Operations

```kotlin
// Pick single PDF
val singlePdfLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    if (uri != null) {
        viewModel.onFileSelected(uri)
    }
}

// Pick multiple PDFs
val multiPdfLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    if (uris.isNotEmpty()) {
        viewModel.onFilesSelected(uris)
    }
}

// Save file
val saveFileLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/pdf")
) { uri ->
    if (uri != null) {
        viewModel.onSaveLocationSelected(uri)
    }
}

// Usage
Button(onClick = {
    singlePdfLauncher.launch(arrayOf("application/pdf"))
}) {
    Text("Select PDF")
}
```

---

## State Restoration with ViewModel

```kotlin
// ViewModel auto-saves state to SavedStateHandle
@HiltViewModel
class MyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val someUseCase: SomeUseCase
) : ViewModel() {
    
    private val _selectedFiles = savedStateHandle.getStateFlow(
        "selectedFiles",
        emptyList<Uri>()
    )
    val selectedFiles = _selectedFiles.asStateFlow()
    
    fun updateSelectedFiles(files: List<Uri>) {
        savedStateHandle["selectedFiles"] = files
    }
}
```

---

## Error Handling with Try-Catch-Flow Pattern

```kotlin
fun performOperationWithErrorHandling() {
    viewModelScope.launch {
        try {
            _uiState.value = UiState.Loading
            
            myUseCase.execute(params)
                .collect { progress ->
                    when (progress.phase) {
                        ProcessingProgress.Phase.READING -> {
                            _uiState.value = UiState.Processing(progress)
                        }
                        ProcessingProgress.Phase.PROCESSING -> {
                            _uiState.value = UiState.Processing(progress)
                        }
                        ProcessingProgress.Phase.WRITING -> {
                            _uiState.value = UiState.Processing(progress)
                        }
                        ProcessingProgress.Phase.DONE -> {
                            _uiState.value = UiState.Success(result)
                        }
                    }
                }
        } catch (e: IOException) {
            _uiState.value = UiState.Error("File error: ${e.message}")
        } catch (e: Exception) {
            _uiState.value = UiState.Error("Unexpected error: ${e.message}")
        }
    }
}
```

---

## Hilt Module for ViewModels

All ViewModels are automatically discovered by Hilt if they have `@HiltViewModel` annotation. No additional module configuration needed!

---

## Testing ViewModels

```kotlin
// src/test/java/com/example/pdf_utility_app/ui/merge/MergeViewModelTest.kt

@RunWith(JUnit4::class)
class MergeViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var mergeUseCase: MergePdfsUseCase
    private lateinit var fileRepository: FileRepository
    private lateinit var viewModel: MergeViewModel
    
    @Before
    fun setup() {
        mergeUseCase = mockk()
        fileRepository = mockk()
        viewModel = MergeViewModel(mergeUseCase, fileRepository)
    }
    
    @Test
    fun testFilesSelected() {
        val documents = listOf(
            PdfDocument("uri1", "file1.pdf", 10, 1000, false),
            PdfDocument("uri2", "file2.pdf", 20, 2000, false)
        )
        
        viewModel.onFilesSelected(documents)
        
        val state = viewModel.uiState.value
        assert(state is MergeUiState.FilesSelected)
        assert((state as MergeUiState.FilesSelected).files.size == 2)
    }
    
    @Test
    fun testPerformMerge() = runTest {
        val documents = listOf(
            PdfDocument("uri1", "file1.pdf", 10, 1000, false)
        )
        viewModel.onFilesSelected(documents)
        
        // Mock the use case
        coEvery {
            mergeUseCase.execute(any())
        } returns flow {
            emit(ProcessingProgress(0, 100, ProcessingProgress.Phase.READING))
            emit(ProcessingProgress(50, 100, ProcessingProgress.Phase.PROCESSING))
            emit(ProcessingProgress(100, 100, ProcessingProgress.Phase.DONE))
        }
        
        viewModel.performMerge("output.pdf")
        
        val state = viewModel.uiState.value
        assert(state is MergeUiState.Success)
    }
}
```

---

These templates should give you a solid foundation to build all screens quickly and consistently!
