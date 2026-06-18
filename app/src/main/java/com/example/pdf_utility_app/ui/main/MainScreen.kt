package com.example.pdf_utility_app.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.pdf_utility_app.AddWatermark
import com.example.pdf_utility_app.CompressPdf
import com.example.pdf_utility_app.DeletePages
import com.example.pdf_utility_app.ExtractPages
import com.example.pdf_utility_app.ExtractText
import com.example.pdf_utility_app.ImagesToPdf
import com.example.pdf_utility_app.MergePdfs
import com.example.pdf_utility_app.PasswordProtect
import com.example.pdf_utility_app.ReorderPages
import com.example.pdf_utility_app.RotatePages
import com.example.pdf_utility_app.SplitPdf
import com.example.pdf_utility_app.UnlockPdf
import androidx.compose.ui.tooling.preview.Preview
import com.example.pdf_utility_app.theme.PdfUtilityAppTheme
import com.example.pdf_utility_app.ui.components.OperationCard

private data class Operation(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val navKey: NavKey
)

private val operations = listOf(
    Operation("Merge PDFs", "Combine multiple PDFs", "📚", MergePdfs),
    Operation("Split PDF", "Divide into parts", "✂️", SplitPdf),
    Operation("Extract Pages", "Pull out pages", "📑", ExtractPages),
    Operation("Delete Pages", "Remove pages", "🗑️", DeletePages),
    Operation("Reorder Pages", "Rearrange order", "🔄", ReorderPages),
    Operation("Rotate Pages", "Rotate 90/180/270°", "🔁", RotatePages),
    Operation("Compress PDF", "Reduce file size", "🗜️", CompressPdf),
    Operation("Add Watermark", "Overlay text", "💧", AddWatermark),
    Operation("Password Protect", "Encrypt PDF", "🔐", PasswordProtect),
    Operation("Unlock PDF", "Remove password", "🔓", UnlockPdf),
    Operation("Extract Text", "Get plain text", "📝", ExtractText),
    Operation("Images to PDF", "Convert images", "🖼️", ImagesToPdf),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PDF Toolkit") })
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Manual 2-column grid — avoids nested lazy list crash
            operations.chunked(2).forEach { rowOps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOps.forEach { op ->
                        OperationCard(
                            title = op.title,
                            subtitle = op.subtitle,
                            emoji = op.emoji,
                            onClick = { onNavigate(op.navKey) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if row has only 1 item
                    if (rowOps.size == 1) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    PdfUtilityAppTheme {
        MainScreen(
            onNavigate = {}
        )
    }
}
