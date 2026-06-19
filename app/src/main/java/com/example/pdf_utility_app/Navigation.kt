package com.example.pdf_utility_app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.pdf_utility_app.ui.compress.CompressScreen
import com.example.pdf_utility_app.ui.delete.DeletePagesScreen
import com.example.pdf_utility_app.ui.extract.ExtractPagesScreen
import com.example.pdf_utility_app.ui.images.ImagesToPdfScreen
import com.example.pdf_utility_app.ui.main.MainScreen
import com.example.pdf_utility_app.ui.merge.MergePdfsScreen
import com.example.pdf_utility_app.ui.password.PasswordProtectScreen
import com.example.pdf_utility_app.ui.reorder.ReorderPagesScreen
import com.example.pdf_utility_app.ui.rotate.RotatePagesScreen
import com.example.pdf_utility_app.ui.split.SplitPdfScreen
import com.example.pdf_utility_app.ui.edit.EditPdfScreen
import com.example.pdf_utility_app.ui.text.ExtractTextScreen
import com.example.pdf_utility_app.ui.unlock.UnlockPdfScreen
import com.example.pdf_utility_app.ui.watermark.AddWatermarkScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)
    val baseModifier = Modifier.fillMaxSize()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(onNavigate = { backStack.add(it) }, modifier = baseModifier)
            }
            entry<MergePdfs> {
                MergePdfsScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<SplitPdf> {
                SplitPdfScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<ExtractPages> {
                ExtractPagesScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<DeletePages> {
                DeletePagesScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<ReorderPages> {
                ReorderPagesScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<RotatePages> {
                RotatePagesScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<CompressPdf> {
                CompressScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<AddWatermark> {
                AddWatermarkScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<PasswordProtect> {
                PasswordProtectScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<UnlockPdf> {
                UnlockPdfScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<ExtractText> {
                ExtractTextScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<ImagesToPdf> {
                ImagesToPdfScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
            entry<EditPdf> {
                EditPdfScreen(onBack = { backStack.removeLastOrNull() }, modifier = baseModifier)
            }
        }
    )
}
