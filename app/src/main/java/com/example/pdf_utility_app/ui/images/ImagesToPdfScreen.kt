package com.example.pdf_utility_app.ui.images

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pdf_utility_app.ui.components.BottomActionBar
import com.example.pdf_utility_app.ui.components.ErrorContent
import com.example.pdf_utility_app.ui.components.PdfTopAppBar
import com.example.pdf_utility_app.ui.components.ProgressContent
import com.example.pdf_utility_app.ui.components.SuccessContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private enum class InputTab { SCAN, GALLERY }

@Composable
fun ImagesToPdfScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImagesToPdfViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(InputTab.SCAN) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}
            }
            viewModel.addImages(uris)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { outputUri ->
        if (outputUri != null) viewModel.convert(outputUri)
    }

    when (val state = uiState) {
        is ImagesToPdfUiState.Processing -> ProgressContent(
            progress = state.progress,
            operationName = "Converting to PDF",
            onCancel = { viewModel.cancel() },
            modifier = modifier
        )
        is ImagesToPdfUiState.Success -> SuccessContent(
            operationName = "Images converted to PDF successfully",
            outputInfo = "Saved to: ${state.outputUri.lastPathSegment}",
            onNewOperation = { viewModel.reset(); onBack() },
            modifier = modifier
        )
        is ImagesToPdfUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.reset() },
            onBack = onBack,
            modifier = modifier
        )
        is ImagesToPdfUiState.Editing -> {
            val images = state.images
            Scaffold(
                topBar = {
                    Column {
                        PdfTopAppBar(title = "Scan / Images to PDF", onBack = onBack)
                        TabRow(selectedTabIndex = activeTab.ordinal) {
                            Tab(
                                selected = activeTab == InputTab.SCAN,
                                onClick = {
                                    activeTab = InputTab.SCAN
                                    if (!hasCameraPermission) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                text = { Text("📷  Scan") }
                            )
                            Tab(
                                selected = activeTab == InputTab.GALLERY,
                                onClick = { activeTab = InputTab.GALLERY },
                                text = { Text("🖼️  Gallery") }
                            )
                        }
                    }
                },
                bottomBar = {
                    Column {
                        if (images.isNotEmpty()) {
                            ImageStrip(images = images, onRemove = { viewModel.removeImage(it) })
                        }
                        BottomActionBar(
                            primaryText = if (images.isEmpty()) "No images yet"
                                          else "Convert ${images.size} image${if (images.size == 1) "" else "s"} to PDF",
                            enabled = images.isNotEmpty(),
                            onPrimary = { saveLauncher.launch("document.pdf") },
                            onCancel = onBack
                        )
                    }
                },
                modifier = modifier
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (activeTab) {
                        InputTab.SCAN -> ScanContent(
                            hasCameraPermission = hasCameraPermission,
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onImageCaptured = { uri -> viewModel.addImages(listOf(uri)) },
                            modifier = Modifier.fillMaxSize()
                        )
                        InputTab.GALLERY -> GalleryContent(
                            imageCount = images.size,
                            onSelectImages = { galleryLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanContent(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onImageCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!hasCameraPermission) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text("📷", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Camera permission is needed to scan documents",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onRequestPermission) { Text("Grant Camera Permission") }
            }
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var isCapturing by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener({
                try {
                    provider = future.get().also { p ->
                        p.unbindAll()
                        p.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    }
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose { provider?.unbindAll() }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    preview.setSurfaceProvider(surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        FloatingActionButton(
            onClick = {
                if (!isCapturing) {
                    isCapturing = true
                    val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                    imageCapture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(file).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onImageCaptured(Uri.fromFile(file))
                                isCapturing = false
                            }
                            override fun onError(e: ImageCaptureException) {
                                isCapturing = false
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            containerColor = if (isCapturing) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.primary
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture document")
            }
        }
    }
}

@Composable
private fun GalleryContent(
    imageCount: Int,
    onSelectImages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🖼️", style = MaterialTheme.typography.displayMedium)
            if (imageCount == 0) {
                Text(
                    "Pick images from your gallery to convert to PDF",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "$imageCount image${if (imageCount == 1) "" else "s"} ready",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Switch to Scan tab to capture more pages, or add more from gallery",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onSelectImages) {
                Text(if (imageCount == 0) "Select Images" else "Add More Images")
            }
        }
    }
}

@Composable
private fun ImageStrip(images: List<Uri>, onRemove: (Uri) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "${images.size} image${if (images.size == 1) "" else "s"} · tap × to remove",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(images, key = { it.toString() }) { uri ->
                ImageThumbnail(uri = uri, onRemove = { onRemove(uri) })
            }
        }
    }
}

@Composable
private fun ImageThumbnail(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            bitmap = try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path, opts)
                } else {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, opts)
                    }
                }
            } catch (_: Exception) { null }
        }
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove image",
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
